package io.roleos.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobCandidate.FilterEvaluation;
import io.roleos.job.domain.JobSearch.JobDecision;
import io.roleos.job.domain.JobTypes.DecisionType;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobDecisionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 用户策略决策必须显式、幂等、用户隔离，并且不能由推荐自动推进。 */
class JobStrategyServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-19T08:00:00Z");

  @Test
  void recommendationDoesNotChangeBroadUntilUserPromotes() {
    MemoryRepository repository = new MemoryRepository();
    UserId userId = UserId.random();
    UUID jobId = UUID.randomUUID();
    JobCandidate candidate = candidate(userId, jobId, StrategyRecommendation.PROMOTE_TO_TARGETED);
    repository.save(candidate);
    JobStrategyService service = service(repository);

    assertThat(repository.find(userId, jobId).orElseThrow().strategy())
        .isEqualTo(JobStrategy.BROAD_APPLY);
    var result =
        assertTimeoutPreemptively(
            Duration.ofSeconds(2),
            () -> service.decide(userId, jobId, "command-0001", DecisionType.PROMOTE, "重点岗位"));

    assertThat(result.candidate().strategy()).isEqualTo(JobStrategy.TARGETED_APPLY);
    assertThat(result.candidate().strategyRecommendation())
        .isEqualTo(StrategyRecommendation.PROMOTE_TO_TARGETED);
  }

  @Test
  void supportsKeepBroadAndSkipAndDoesNotTouchCanonicalJob() {
    MemoryRepository repository = new MemoryRepository();
    UserId userId = UserId.random();
    UUID jobId = UUID.randomUUID();
    repository.save(candidate(userId, jobId, StrategyRecommendation.KEEP_BROAD));
    JobStrategyService service = service(repository);

    assertThat(
            service
                .decide(userId, jobId, "command-0002", DecisionType.KEEP_BROAD, null)
                .candidate()
                .strategy())
        .isEqualTo(JobStrategy.BROAD_APPLY);
    assertThat(
            service
                .decide(userId, jobId, "command-0003", DecisionType.SKIP, "暂不考虑")
                .candidate()
                .strategy())
        .isEqualTo(JobStrategy.SKIPPED);
    assertThat(repository.canonicalWrites).isZero();
  }

  @Test
  void repeatedCommandIsIdempotentAndConflictingReuseIsRejected() {
    MemoryRepository repository = new MemoryRepository();
    UserId userId = UserId.random();
    UUID jobId = UUID.randomUUID();
    repository.save(candidate(userId, jobId, StrategyRecommendation.KEEP_BROAD));
    JobStrategyService service = service(repository);

    var first = service.decide(userId, jobId, "command-0004", DecisionType.SKIP, null);
    var repeated = service.decide(userId, jobId, "command-0004", DecisionType.SKIP, null);

    assertThat(repeated.decision().id()).isEqualTo(first.decision().id());
    assertThat(repository.decisions).hasSize(1);
    assertThatThrownBy(
            () -> service.decide(userId, jobId, "command-0004", DecisionType.PROMOTE, null))
        .isInstanceOfSatisfying(
            JobStrategyService.JobStrategyException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(JobStrategyService.FailureReason.CONFLICT));
  }

  @Test
  void anotherUserCannotDecideOwnersCandidate() {
    MemoryRepository repository = new MemoryRepository();
    UserId owner = UserId.random();
    UUID jobId = UUID.randomUUID();
    repository.save(candidate(owner, jobId, StrategyRecommendation.KEEP_BROAD));

    assertThatThrownBy(
            () ->
                service(repository)
                    .decide(UserId.random(), jobId, "command-0005", DecisionType.SKIP, null))
        .isInstanceOfSatisfying(
            JobStrategyService.JobStrategyException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(JobStrategyService.FailureReason.NOT_FOUND));
  }

  private JobStrategyService service(MemoryRepository repository) {
    return new JobStrategyService(repository, repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private JobCandidate candidate(UserId userId, UUID jobId, StrategyRecommendation recommendation) {
    return new JobCandidate(
        UUID.randomUUID(),
        userId,
        jobId,
        FilterResult.PASS,
        null,
        JobStrategy.BROAD_APPLY,
        recommendation,
        SemanticAnalysisStatus.SUCCEEDED,
        null,
        Map.of(),
        0,
        NOW,
        NOW);
  }

  private static final class MemoryRepository
      implements JobCandidateRepository, JobDecisionRepository {
    private final Map<UUID, JobCandidate> candidates = new LinkedHashMap<>();
    private final List<JobDecision> decisions = new ArrayList<>();
    private int canonicalWrites;

    @Override
    public Optional<JobCandidate> find(UserId userId, UUID jobId) {
      return candidates.values().stream()
          .filter(value -> value.userId().equals(userId) && value.jobId().equals(jobId))
          .findFirst();
    }

    @Override
    public List<JobCandidate> findAll(
        UserId userId, JobStrategy strategy, FilterResult filterResult, int offset, int limit) {
      return List.of();
    }

    @Override
    public long count(UserId userId, JobStrategy strategy, FilterResult filterResult) {
      return 0;
    }

    @Override
    public JobCandidate save(JobCandidate candidate) {
      candidates.put(candidate.id(), candidate);
      return candidate;
    }

    @Override
    public FilterEvaluation saveFilterEvaluation(FilterEvaluation evaluation) {
      return evaluation;
    }

    @Override
    public Optional<FilterEvaluation> findFilterEvaluation(UUID candidateId) {
      return Optional.empty();
    }

    @Override
    public Optional<JobDecision> findByCommandId(UserId userId, String commandId) {
      return decisions.stream()
          .filter(value -> value.userId().equals(userId) && value.commandId().equals(commandId))
          .findFirst();
    }

    @Override
    public JobDecision save(JobDecision decision, JobCandidate resultingCandidate) {
      save(resultingCandidate);
      decisions.add(decision);
      return decision;
    }
  }
}
