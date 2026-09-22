package io.roleos.application.career.skill;

import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.IdempotencyRecord;
import io.roleos.domain.career.IdempotencyRecordPort;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.skill.Capability;
import io.roleos.domain.career.skill.CapabilityLinks;
import io.roleos.domain.career.skill.Skill;
import io.roleos.domain.career.skill.SkillProvenanceRepository;
import io.roleos.domain.career.skill.SkillSource;
import io.roleos.domain.career.skill.SkillSourceType;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Explicit skill-source and capability maintenance; no level or score is inferred here. */
@Service
@SuppressWarnings(
    "EI_EXPOSE_REP2") // Spring-managed Port/Repository collaborators are retained by design.
public final class SkillProvenanceApplicationService {
  private static final String LOG_COMMAND_ID = "commandId";
  private static final String LOG_EVENT = "event";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SkillProvenanceApplicationService.class);

  private final io.roleos.domain.career.CareerAssetRepository assets;
  private final SkillProvenanceRepository repository;
  private final IdempotencyRecordPort idempotency;
  private final Clock clock;

  public SkillProvenanceApplicationService(
      io.roleos.domain.career.CareerAssetRepository assets,
      SkillProvenanceRepository repository,
      IdempotencyRecordPort idempotency,
      Clock clock) {
    this.assets = assets;
    this.repository = repository;
    this.idempotency = idempotency;
    this.clock = clock;
  }

  public List<SkillView> skills(UserId userId) {
    return assets.findSkills(userId).stream()
        .map(skill -> new SkillView(skill, repository.findSources(userId, skill.id())))
        .toList();
  }

  public List<CapabilityView> capabilities(UserId userId) {
    return repository.findCapabilities(userId).stream()
        .map(c -> new CapabilityView(c, repository.links(userId, c.id())))
        .toList();
  }

  public SkillSource addSource(UserId userId, AddSourceCommand command) {
    Skill skill =
        assets
            .findSkill(userId, command.skillId())
            .orElseThrow(() -> new IllegalArgumentException("技能不存在或不属于当前用户"));
    var old = idempotency.findBy(userId, command.commandId());
    if (old.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "career.skill_source.idempotency_hit")
          .addKeyValue("skillId", skill.id())
          .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
          .log("技能来源写入命令已幂等命中");
      return repository.findSources(userId, skill.id()).stream()
          .filter(s -> s.id().toString().equals(old.orElseThrow().resultReference()))
          .findFirst()
          .orElseThrow();
    }
    SkillSource source =
        new SkillSource(
            UUID.randomUUID(),
            skill.id(),
            command.type(),
            command.supportingAssetReference(),
            clock.instant());
    repository.saveSource(userId, source);
    idempotency.saveIfAbsent(
        new IdempotencyRecord(
            userId, command.commandId(), source.id().toString(), clock.instant()));
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "career.skill_source.added")
        .addKeyValue("skillId", skill.id())
        .addKeyValue("sourceId", source.id())
        .addKeyValue("sourceType", command.type())
        .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
        .log("技能来源已保存");
    return source;
  }

  public CapabilityView saveCapability(UserId userId, SaveCapabilityCommand command) {
    var previous = idempotency.findBy(userId, command.commandId());
    if (previous.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "career.capability.idempotency_hit")
          .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
          .log("能力写入命令已幂等命中");
      Capability capability =
          repository
              .findCapability(userId, UUID.fromString(previous.orElseThrow().resultReference()))
              .orElseThrow(() -> new IllegalStateException("幂等能力结果不存在"));
      return new CapabilityView(capability, repository.links(userId, capability.id()));
    }
    Capability capability =
        repository.saveCapability(
            new Capability(
                command.id() == null ? UUID.randomUUID() : command.id(), userId, command.name()));
    CapabilityLinks links =
        repository.replaceLinks(
            userId,
            new CapabilityLinks(
                capability.id(),
                command.skillIds(),
                command.experienceIds(),
                command.projectIds()));
    idempotency.saveIfAbsent(
        new IdempotencyRecord(
            userId, command.commandId(), capability.id().toString(), clock.instant()));
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "career.capability.saved")
        .addKeyValue("capabilityId", capability.id())
        .addKeyValue(LOG_COMMAND_ID, command.commandId().value())
        .addKeyValue("skillCount", links.skillIds().size())
        .addKeyValue("experienceCount", links.experienceIds().size())
        .addKeyValue("projectCount", links.projectIds().size())
        .log("能力及其关联已保存");
    return new CapabilityView(capability, links);
  }

  public record AddSourceCommand(
      CommandId commandId, UUID skillId, SkillSourceType type, String supportingAssetReference) {}

  public record SaveCapabilityCommand(
      CommandId commandId,
      UUID id,
      String name,
      List<UUID> skillIds,
      List<UUID> experienceIds,
      List<UUID> projectIds) {
    public SaveCapabilityCommand {
      skillIds = List.copyOf(skillIds);
      experienceIds = List.copyOf(experienceIds);
      projectIds = List.copyOf(projectIds);
    }
  }

  public record SkillView(Skill skill, List<SkillSource> sources) {
    public SkillView {
      sources = List.copyOf(sources);
    }
  }

  public record CapabilityView(Capability capability, CapabilityLinks links) {}
}
