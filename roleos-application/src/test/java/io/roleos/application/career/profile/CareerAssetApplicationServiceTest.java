package io.roleos.application.career.profile;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.roleos.domain.career.skill.Skill;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareerAssetApplicationServiceTest {
  @Test
  void createsConfirmedUserInputAndReturnsSameResultForDuplicateCommand() {
    MemoryAssets assets = new MemoryAssets();
    MemoryIdempotency idempotency = new MemoryIdempotency();
    CareerAssetApplicationService service =
        new CareerAssetApplicationService(
            assets,
            idempotency,
            Clock.fixed(Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC));
    UserId userId = UserId.random();
    CommandId commandId = new CommandId(UUID.randomUUID());
    var command =
        new CareerAssetApplicationService.ExperienceCommand(
            commandId, null, ExperienceType.EMPLOYMENT, "工程师", "示例组织", false);

    Experience first = service.saveExperience(userId, command);
    Experience duplicate = service.saveExperience(userId, command);

    assertThat(duplicate.id()).isEqualTo(first.id());
    assertThat(first.userId()).isEqualTo(userId);
    assertThat(first.provenanceType()).isEqualTo(ProvenanceType.USER_INPUT);
    assertThat(first.confirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
    assertThat(assets.experiences).hasSize(1);
  }

  private static final class MemoryIdempotency implements IdempotencyRecordPort {
    private final Map<String, IdempotencyRecord> values = new HashMap<>();

    @Override
    public Optional<IdempotencyRecord> findBy(UserId userId, CommandId commandId) {
      return Optional.ofNullable(values.get(userId.value() + ":" + commandId.value()));
    }

    @Override
    public IdempotencyRecord saveIfAbsent(IdempotencyRecord record) {
      values.putIfAbsent(record.userId().value() + ":" + record.commandId().value(), record);
      return findBy(record.userId(), record.commandId()).orElseThrow();
    }
  }

  private static final class MemoryAssets implements CareerAssetRepository {
    private final List<Experience> experiences = new ArrayList<>();
    private final List<Project> projects = new ArrayList<>();
    private final List<Skill> skills = new ArrayList<>();

    public List<Experience> findExperiences(UserId userId) {
      return experiences.stream().filter(v -> v.userId().equals(userId)).toList();
    }

    public Optional<Experience> findExperience(UserId userId, UUID id) {
      return findExperiences(userId).stream().filter(v -> v.id().equals(id)).findFirst();
    }

    public Experience save(Experience value) {
      experiences.removeIf(v -> v.id().equals(value.id()));
      experiences.add(value);
      return value;
    }

    public boolean deleteExperience(UserId userId, UUID id) {
      return experiences.removeIf(v -> v.userId().equals(userId) && v.id().equals(id));
    }

    public List<Project> findProjects(UserId userId) {
      return projects.stream().filter(v -> v.userId().equals(userId)).toList();
    }

    public Optional<Project> findProject(UserId userId, UUID id) {
      return findProjects(userId).stream().filter(v -> v.id().equals(id)).findFirst();
    }

    public Project save(Project value) {
      projects.removeIf(v -> v.id().equals(value.id()));
      projects.add(value);
      return value;
    }

    public boolean deleteProject(UserId userId, UUID id) {
      return projects.removeIf(v -> v.userId().equals(userId) && v.id().equals(id));
    }

    public List<Skill> findSkills(UserId userId) {
      return skills.stream().filter(v -> v.userId().equals(userId)).toList();
    }

    public Optional<Skill> findSkill(UserId userId, UUID id) {
      return findSkills(userId).stream().filter(v -> v.id().equals(id)).findFirst();
    }

    public Skill save(Skill value) {
      skills.removeIf(v -> v.id().equals(value.id()));
      skills.add(value);
      return value;
    }

    public boolean deleteSkill(UserId userId, UUID id) {
      return skills.removeIf(v -> v.userId().equals(userId) && v.id().equals(id));
    }
  }
}
