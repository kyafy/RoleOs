package io.roleos.application.career.profile;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.CareerProfileRepository;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.IdempotencyRecord;
import io.roleos.domain.career.IdempotencyRecordPort;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.profile.CareerProfile;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 职业档案用例；用户归属由调用方传入的认证主体决定。 */
@Service
public final class CareerProfileApplicationService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CareerProfileApplicationService.class);

  private final CareerProfileRepository profileRepository;
  private final IdempotencyRecordPort idempotencyRecordPort;
  private final Clock clock;

  public CareerProfileApplicationService(
      CareerProfileRepository profileRepository,
      IdempotencyRecordPort idempotencyRecordPort,
      Clock clock) {
    this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository 不能为空");
    this.idempotencyRecordPort =
        Objects.requireNonNull(idempotencyRecordPort, "idempotencyRecordPort 不能为空");
    this.clock = Objects.requireNonNull(clock, "clock 不能为空");
  }

  public CareerProfile get(UserId currentUser) {
    return profileRepository
        .findByUserId(currentUser)
        .orElseThrow(() -> new IllegalArgumentException("当前用户尚未创建职业档案"));
  }

  public CareerProfile upsert(UserId currentUser, UpsertCareerProfileCommand command) {
    Objects.requireNonNull(currentUser, "currentUser 不能为空");
    Objects.requireNonNull(command, "command 不能为空");
    if (idempotencyRecordPort.findBy(currentUser, command.commandId()).isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue("event", "career.profile.idempotency_hit")
          .addKeyValue("commandId", command.commandId().value())
          .log("职业档案更新命令已幂等命中");
      return get(currentUser);
    }

    Instant now = clock.instant();
    CareerProfile saved =
        profileRepository.save(
            profileRepository
                .findByUserId(currentUser)
                .map(profile -> update(profile, command, now))
                .orElseGet(() -> create(currentUser, command, now)));
    idempotencyRecordPort.saveIfAbsent(
        new IdempotencyRecord(currentUser, command.commandId(), saved.id().toString(), now));
    LOGGER
        .atInfo()
        .addKeyValue("event", "career.profile.upserted")
        .addKeyValue("profileId", saved.id())
        .addKeyValue("commandId", command.commandId().value())
        .addKeyValue("outcome", saved.auditFields().version() == 0 ? "CREATED" : "UPDATED")
        .log("职业档案已保存");
    return saved;
  }

  private CareerProfile create(UserId userId, UpsertCareerProfileCommand command, Instant now) {
    return new CareerProfile(
        UUID.randomUUID(),
        userId,
        command.displayName(),
        command.careerDirection(),
        command.targetRolePreference(),
        ProvenanceType.USER_INPUT,
        ConfirmationStatus.CONFIRMED,
        new AuditFields(now, now, 0));
  }

  private CareerProfile update(
      CareerProfile profile, UpsertCareerProfileCommand command, Instant now) {
    AuditFields existing = profile.auditFields();
    return new CareerProfile(
        profile.id(),
        profile.userId(),
        command.displayName(),
        command.careerDirection(),
        command.targetRolePreference(),
        ProvenanceType.USER_INPUT,
        ConfirmationStatus.CONFIRMED,
        new AuditFields(existing.createdAt(), now, existing.version() + 1));
  }
}
