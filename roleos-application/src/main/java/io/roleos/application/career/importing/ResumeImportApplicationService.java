package io.roleos.application.career.importing;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.CareerAssetRepository;
import io.roleos.domain.career.CareerFactSource;
import io.roleos.domain.career.CareerFactSourceRepository;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.IdempotencyRecord;
import io.roleos.domain.career.IdempotencyRecordPort;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.importing.CandidateDecision;
import io.roleos.domain.career.importing.CandidateDecisionType;
import io.roleos.domain.career.importing.FactCandidate;
import io.roleos.domain.career.importing.ResumeImport;
import io.roleos.domain.career.importing.ResumeImportRepository;
import io.roleos.domain.career.profile.Experience;
import io.roleos.domain.career.profile.ExperienceType;
import io.roleos.domain.career.profile.Project;
import io.roleos.domain.career.profile.ProjectNature;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import io.roleos.domain.career.skill.Skill;
import io.roleos.experience.importer.ResumeImportFixture;
import io.roleos.experience.importer.ResumeImportPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates controlled import review; extraction output never becomes a fact automatically. */
@Service
@SuppressWarnings(
    "EI_EXPOSE_REP2") // Spring-managed Port/Repository collaborators are retained by design.
public class ResumeImportApplicationService {
  private static final String LOG_COMMAND_ID = "commandId";
  private static final String LOG_EVENT = "event";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ResumeImportApplicationService.class);

  private final ResumeImportPort importer;
  private final ResumeImportRepository imports;
  private final CareerAssetRepository assets;
  private final CareerFactSourceRepository sources;
  private final IdempotencyRecordPort idempotency;
  private final Clock clock;

  public ResumeImportApplicationService(
      ResumeImportPort importer,
      ResumeImportRepository imports,
      CareerAssetRepository assets,
      CareerFactSourceRepository sources,
      IdempotencyRecordPort idempotency,
      Clock clock) {
    this.importer = Objects.requireNonNull(importer);
    this.imports = Objects.requireNonNull(imports);
    this.assets = Objects.requireNonNull(assets);
    this.sources = Objects.requireNonNull(sources);
    this.idempotency = Objects.requireNonNull(idempotency);
    this.clock = Objects.requireNonNull(clock);
  }

  @Transactional
  public CreateResult create(UserId userId, CreateCommand command) {
    var previous = idempotency.findBy(userId, command.commandId());
    if (previous.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "career.resume_import.idempotency_hit")
          .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
          .log("简历导入命令已幂等命中");
      return new CreateResult(
          imports
              .findImport(userId, UUID.fromString(previous.orElseThrow().resultReference()))
              .orElseThrow(() -> new IllegalStateException("幂等导入结果不存在")),
          List.of());
    }
    var extraction =
        importer.extract(
            new io.roleos.experience.importer.ResumeImportRequest(
                command.controlledSourceReference(), command.fixtures()));
    ResumeImport resumeImport =
        new ResumeImport(
            UUID.randomUUID(),
            userId,
            command.controlledSourceReference(),
            clock.instant(),
            extraction.candidates());
    imports.save(resumeImport);
    idempotency.saveIfAbsent(
        new IdempotencyRecord(
            userId, command.commandId(), resumeImport.id().toString(), clock.instant()));
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "career.resume_import.created")
        .addKeyValue("importId", resumeImport.id())
        .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
        .addKeyValue("candidateCount", extraction.candidates().size())
        .addKeyValue("failureCount", extraction.failures().size())
        .log("简历导入候选项已创建，仍需人工确认");
    return new CreateResult(resumeImport, extraction.failures());
  }

  @Transactional
  public DecisionResult decide(UserId userId, UUID candidateId, DecisionCommand command) {
    FactCandidate candidate =
        imports
            .findCandidate(userId, candidateId)
            .orElseThrow(() -> new IllegalArgumentException("候选项不存在或不属于当前用户"));
    var previous = idempotency.findBy(userId, command.commandId());
    if (previous.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "career.resume_candidate.idempotency_hit")
          .addKeyValue("candidateId", candidateId)
          .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
          .log("简历候选项决定已幂等命中");
      return DecisionResult.from(
          imports
              .findCandidate(userId, UUID.fromString(previous.orElseThrow().resultReference()))
              .orElseThrow(() -> new IllegalStateException("幂等候选项结果不存在")));
    }
    if (candidate.status()
        != io.roleos.domain.career.importing.CandidateReviewStatus.WAITING_CONFIRMATION) {
      throw new IllegalStateException("候选项当前状态不允许作出决定");
    }

    String payload =
        command.type() == CandidateDecisionType.EDIT
            ? command.editedPayload()
            : candidate.payload();
    if (command.type() != CandidateDecisionType.REJECT) promote(userId, candidate, payload);
    CandidateDecision decision =
        new CandidateDecision(
            candidateId,
            command.type(),
            command.editedPayload(),
            command.commandId().value(),
            clock.instant());
    imports.saveDecision(userId, decision);
    idempotency.saveIfAbsent(
        new IdempotencyRecord(
            userId, command.commandId(), candidateId.toString(), clock.instant()));
    candidate.decide(decision);
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "career.resume_candidate.decided")
        .addKeyValue("candidateId", candidateId)
        .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
        .addKeyValue("decision", command.type())
        .addKeyValue("status", candidate.status())
        .log("简历候选项人工决定已持久化");
    return DecisionResult.from(candidate);
  }

  private void promote(UserId userId, FactCandidate candidate, String payload) {
    Instant now = clock.instant();
    UUID assetId =
        switch (candidate.candidateType()) {
          case "EXPERIENCE" -> saveExperience(userId, payload, now).id();
          case "PROJECT" -> saveProject(userId, payload, now).id();
          case "SKILL" -> saveSkill(userId, payload, now).id();
          default -> throw new IllegalArgumentException("不支持的候选类型");
        };
    sources.append(
        new CareerFactSource(
            UUID.randomUUID(),
            userId,
            candidate.candidateType(),
            assetId,
            ProvenanceType.USER_CONFIRMED,
            ConfirmationStatus.CONFIRMED,
            "resume-candidate:" + candidate.id(),
            now));
  }

  private Experience saveExperience(UserId userId, String payload, Instant now) {
    return assets.save(
        new Experience(
            UUID.randomUUID(),
            userId,
            ExperienceType.valueOf(requiredString(payload, "type")),
            requiredString(payload, "title"),
            requiredString(payload, "organization"),
            optionalBoolean(payload, "incomplete", true),
            ProvenanceType.USER_CONFIRMED,
            ConfirmationStatus.CONFIRMED,
            new AuditFields(now, now, 0)));
  }

  private Project saveProject(UserId userId, String payload, Instant now) {
    return assets.save(
        new Project(
            UUID.randomUUID(),
            userId,
            requiredString(payload, "name"),
            ProjectNature.valueOf(requiredString(payload, "nature")),
            requiredString(payload, "historicalSource"),
            ProvenanceType.USER_CONFIRMED,
            ConfirmationStatus.CONFIRMED,
            new AuditFields(now, now, 0)));
  }

  private Skill saveSkill(UserId userId, String payload, Instant now) {
    String displayName = requiredString(payload, "displayName");
    String normalizedName = Skill.normalize(displayName);
    return assets.findSkills(userId).stream()
        .filter(skill -> skill.normalizedName().equals(normalizedName))
        .findFirst()
        .orElseGet(
            () ->
                assets.save(
                    new Skill(
                        UUID.randomUUID(),
                        userId,
                        displayName,
                        normalizedName,
                        SelfAssessmentLevel.valueOf(requiredString(payload, "selfAssessmentLevel")),
                        null,
                        ProvenanceType.USER_CONFIRMED,
                        ConfirmationStatus.CONFIRMED,
                        new AuditFields(now, now, 0))));
  }

  private String requiredString(String payload, String key) {
    Matcher matcher = stringField(key).matcher(payload);
    if (!matcher.find() || matcher.group(1).isBlank())
      throw new IllegalArgumentException("候选项缺少 " + key);
    return matcher.group(1);
  }

  private boolean optionalBoolean(String payload, String key, boolean defaultValue) {
    Matcher matcher = booleanField(key).matcher(payload);
    return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : defaultValue;
  }

  private Pattern stringField(String key) {
    return Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
  }

  private Pattern booleanField(String key) {
    return Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)");
  }

  public record CreateCommand(
      CommandId commandId, String controlledSourceReference, List<ResumeImportFixture> fixtures) {
    public CreateCommand {
      fixtures = List.copyOf(fixtures);
    }
  }

  public record CreateResult(ResumeImport resumeImport, List<String> failures) {
    public CreateResult {
      failures = List.copyOf(failures);
    }
  }

  public record DecisionCommand(
      CommandId commandId, CandidateDecisionType type, String editedPayload) {}

  public record DecisionResult(UUID candidateId, String status) {
    static DecisionResult from(FactCandidate candidate) {
      return new DecisionResult(candidate.id(), candidate.status().name());
    }
  }
}
