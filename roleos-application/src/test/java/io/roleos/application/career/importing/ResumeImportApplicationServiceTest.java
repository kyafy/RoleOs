package io.roleos.application.career.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.roleos.domain.career.CareerAssetRepository;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.IdempotencyRecord;
import io.roleos.domain.career.IdempotencyRecordPort;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.importing.CandidateDecision;
import io.roleos.domain.career.importing.CandidateDecisionType;
import io.roleos.domain.career.importing.FactCandidate;
import io.roleos.domain.career.importing.ResumeImport;
import io.roleos.domain.career.importing.ResumeImportRepository;
import io.roleos.domain.career.profile.Experience;
import io.roleos.domain.career.profile.Project;
import io.roleos.domain.career.skill.Skill;
import io.roleos.experience.importer.FakeResumeImportAdapter;
import io.roleos.experience.importer.ResumeImportFixture;
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

class ResumeImportApplicationServiceTest {

  @Test
  void onlyConfirmedAndEditedCandidatesBecomeCareerFacts() {
    InMemoryImports imports = new InMemoryImports();
    InMemoryAssets assets = new InMemoryAssets();
    ResumeImportApplicationService service =
        new ResumeImportApplicationService(
            new FakeResumeImportAdapter(),
            imports,
            assets,
            source -> source,
            new InMemoryIdempotency(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    UserId userId = UserId.random();
    var created =
        service.create(
            userId,
            new ResumeImportApplicationService.CreateCommand(
                new CommandId(UUID.randomUUID()),
                "fixture://resume/test",
                List.of(
                    new ResumeImportFixture(
                        "SKILL",
                        "{\"displayName\":\"Java\",\"selfAssessmentLevel\":\"WORKING\"}",
                        "skills:1"),
                    new ResumeImportFixture(
                        "SKILL",
                        "{\"displayName\":\"Python\",\"selfAssessmentLevel\":\"USED\"}",
                        "skills:2"),
                    new ResumeImportFixture(
                        "SKILL",
                        "{\"displayName\":\"Go\",\"selfAssessmentLevel\":\"AWARENESS\"}",
                        "skills:3"))));

    service.decide(
        userId,
        created.resumeImport().candidates().get(0).id(),
        new ResumeImportApplicationService.DecisionCommand(
            new CommandId(UUID.randomUUID()), CandidateDecisionType.CONFIRM, null));
    service.decide(
        userId,
        created.resumeImport().candidates().get(1).id(),
        new ResumeImportApplicationService.DecisionCommand(
            new CommandId(UUID.randomUUID()),
            CandidateDecisionType.EDIT,
            "{\"displayName\":\"Python 3\",\"selfAssessmentLevel\":\"WORKING\"}"));
    service.decide(
        userId,
        created.resumeImport().candidates().get(2).id(),
        new ResumeImportApplicationService.DecisionCommand(
            new CommandId(UUID.randomUUID()), CandidateDecisionType.REJECT, null));

    assertEquals(2, assets.skills.size());
    assertEquals("java", assets.skills.getFirst().normalizedName());
    assertEquals("python 3", assets.skills.get(1).normalizedName());
  }

  private static final class InMemoryImports implements ResumeImportRepository {
    private final Map<UUID, ResumeImport> imports = new HashMap<>();
    private final Map<UUID, FactCandidate> candidates = new HashMap<>();

    @Override
    public ResumeImport save(ResumeImport value) {
      imports.put(value.id(), value);
      value.candidates().forEach(candidate -> candidates.put(candidate.id(), candidate));
      return value;
    }

    @Override
    public Optional<ResumeImport> findImport(UserId userId, UUID importId) {
      return Optional.ofNullable(imports.get(importId))
          .filter(value -> value.userId().equals(userId));
    }

    @Override
    public Optional<FactCandidate> findCandidate(UserId userId, UUID candidateId) {
      return candidates.containsKey(candidateId)
          ? Optional.of(candidates.get(candidateId))
          : Optional.empty();
    }

    @Override
    public CandidateDecision saveDecision(UserId userId, CandidateDecision decision) {
      return decision;
    }
  }

  private static final class InMemoryAssets implements CareerAssetRepository {
    private final List<Skill> skills = new ArrayList<>();

    @Override
    public List<Experience> findExperiences(UserId userId) {
      return List.of();
    }

    @Override
    public Optional<Experience> findExperience(UserId userId, UUID id) {
      return Optional.empty();
    }

    @Override
    public Experience save(Experience value) {
      return value;
    }

    @Override
    public boolean deleteExperience(UserId userId, UUID id) {
      return false;
    }

    @Override
    public List<Project> findProjects(UserId userId) {
      return List.of();
    }

    @Override
    public Optional<Project> findProject(UserId userId, UUID id) {
      return Optional.empty();
    }

    @Override
    public Project save(Project value) {
      return value;
    }

    @Override
    public boolean deleteProject(UserId userId, UUID id) {
      return false;
    }

    @Override
    public List<Skill> findSkills(UserId userId) {
      return skills;
    }

    @Override
    public Optional<Skill> findSkill(UserId userId, UUID id) {
      return Optional.empty();
    }

    @Override
    public Skill save(Skill value) {
      skills.add(value);
      return value;
    }

    @Override
    public boolean deleteSkill(UserId userId, UUID id) {
      return false;
    }
  }

  private static final class InMemoryIdempotency implements IdempotencyRecordPort {
    private final Map<String, IdempotencyRecord> values = new HashMap<>();

    @Override
    public Optional<IdempotencyRecord> findBy(UserId userId, CommandId commandId) {
      return Optional.ofNullable(values.get(commandId.value().toString()));
    }

    @Override
    public IdempotencyRecord saveIfAbsent(IdempotencyRecord record) {
      values.putIfAbsent(record.commandId().value().toString(), record);
      return values.get(record.commandId().value().toString());
    }
  }
}
