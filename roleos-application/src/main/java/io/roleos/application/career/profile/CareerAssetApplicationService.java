package io.roleos.application.career.profile;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.CareerAssetRepository;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.IdempotencyRecord;
import io.roleos.domain.career.IdempotencyRecordPort;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.profile.Experience;
import io.roleos.domain.career.profile.ExperienceType;
import io.roleos.domain.career.profile.Project;
import io.roleos.domain.career.profile.ProjectNature;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import io.roleos.domain.career.skill.Skill;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 当前认证用户的职业资产 CRUD；写入固定为用户输入且已确认。 */
@Service
@SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed Port collaborators are retained by design.
public final class CareerAssetApplicationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CareerAssetApplicationService.class);

  private final CareerAssetRepository repository;
  private final IdempotencyRecordPort idempotency;
  private final Clock clock;

  public CareerAssetApplicationService(
      CareerAssetRepository repository, IdempotencyRecordPort idempotency, Clock clock) {
    this.repository = Objects.requireNonNull(repository);
    this.idempotency = Objects.requireNonNull(idempotency);
    this.clock = Objects.requireNonNull(clock);
  }

  public List<Experience> experiences(UserId userId) {
    return repository.findExperiences(userId);
  }

  public List<Project> projects(UserId userId) {
    return repository.findProjects(userId);
  }

  public List<Skill> skills(UserId userId) {
    return repository.findSkills(userId);
  }

  public Experience saveExperience(UserId userId, ExperienceCommand command) {
    return once(
        userId,
        command.commandId(),
        id -> repository.findExperience(userId, id),
        () -> {
          Instant now = clock.instant();
          Experience previous =
              command.id() == null
                  ? null
                  : repository.findExperience(userId, command.id()).orElse(null);
          return repository.save(
              new Experience(
                  previous == null ? UUID.randomUUID() : previous.id(),
                  userId,
                  command.type(),
                  command.title(),
                  command.organization(),
                  command.incomplete(),
                  ProvenanceType.USER_INPUT,
                  ConfirmationStatus.CONFIRMED,
                  audit(previous == null ? null : previous.auditFields(), now)));
        });
  }

  public Project saveProject(UserId userId, ProjectCommand command) {
    return once(
        userId,
        command.commandId(),
        id -> repository.findProject(userId, id),
        () -> {
          Instant now = clock.instant();
          Project previous =
              command.id() == null
                  ? null
                  : repository.findProject(userId, command.id()).orElse(null);
          return repository.save(
              new Project(
                  previous == null ? UUID.randomUUID() : previous.id(),
                  userId,
                  command.name(),
                  command.nature(),
                  command.historicalSource(),
                  ProvenanceType.USER_INPUT,
                  ConfirmationStatus.CONFIRMED,
                  audit(previous == null ? null : previous.auditFields(), now)));
        });
  }

  public Skill saveSkill(UserId userId, SkillCommand command) {
    return once(
        userId,
        command.commandId(),
        id -> repository.findSkill(userId, id),
        () -> {
          Instant now = clock.instant();
          Skill previous =
              command.id() == null ? null : repository.findSkill(userId, command.id()).orElse(null);
          return repository.save(
              new Skill(
                  previous == null ? UUID.randomUUID() : previous.id(),
                  userId,
                  command.displayName(),
                  Skill.normalize(command.displayName()),
                  command.selfAssessmentLevel(),
                  previous == null ? null : previous.verifiedLevel(),
                  ProvenanceType.USER_INPUT,
                  ConfirmationStatus.CONFIRMED,
                  audit(previous == null ? null : previous.auditFields(), now)));
        });
  }

  public boolean deleteExperience(UserId userId, UUID id) {
    return delete(userId, id, "EXPERIENCE", repository::deleteExperience);
  }

  public boolean deleteProject(UserId userId, UUID id) {
    return delete(userId, id, "PROJECT", repository::deleteProject);
  }

  public boolean deleteSkill(UserId userId, UUID id) {
    return delete(userId, id, "SKILL", repository::deleteSkill);
  }

  private AuditFields audit(AuditFields previous, Instant now) {
    return previous == null
        ? new AuditFields(now, now, 0)
        : new AuditFields(previous.createdAt(), now, previous.version() + 1);
  }

  private <T> T once(
      UserId userId,
      CommandId commandId,
      Function<UUID, java.util.Optional<T>> existingResult,
      Supplier<T> action) {
    Objects.requireNonNull(userId);
    Objects.requireNonNull(commandId);
    var previousCommand = idempotency.findBy(userId, commandId);
    if (previousCommand.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue("event", "career.asset.idempotency_hit")
          .addKeyValue("commandId", commandId.value())
          .log("职业资产写入命令已幂等命中");
      return existingResult
          .apply(UUID.fromString(previousCommand.orElseThrow().resultReference()))
          .orElseThrow(() -> new IllegalStateException("幂等结果引用不存在"));
    }
    T result = action.get();
    String reference =
        result instanceof Experience e
            ? e.id().toString()
            : result instanceof Project p ? p.id().toString() : ((Skill) result).id().toString();
    idempotency.saveIfAbsent(new IdempotencyRecord(userId, commandId, reference, clock.instant()));
    LOGGER
        .atInfo()
        .addKeyValue("event", "career.asset.saved")
        .addKeyValue("assetId", reference)
        .addKeyValue("assetType", result.getClass().getSimpleName())
        .addKeyValue("commandId", commandId.value())
        .log("职业资产已保存");
    return result;
  }

  private boolean delete(
      UserId userId,
      UUID id,
      String assetType,
      java.util.function.BiFunction<UserId, UUID, Boolean> action) {
    boolean deleted = action.apply(userId, id);
    if (deleted) {
      LOGGER
          .atInfo()
          .addKeyValue("event", "career.asset.deleted")
          .addKeyValue("assetId", id)
          .addKeyValue("assetType", assetType)
          .log("职业资产已删除");
    }
    return deleted;
  }

  public record ExperienceCommand(
      CommandId commandId,
      UUID id,
      ExperienceType type,
      String title,
      String organization,
      boolean incomplete) {}

  public record ProjectCommand(
      CommandId commandId, UUID id, String name, ProjectNature nature, String historicalSource) {}

  public record SkillCommand(
      CommandId commandId, UUID id, String displayName, SelfAssessmentLevel selfAssessmentLevel) {}
}
