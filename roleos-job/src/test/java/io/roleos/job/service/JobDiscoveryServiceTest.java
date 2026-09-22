package io.roleos.job.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobCandidate.FilterEvaluation;
import io.roleos.job.domain.JobSearch.SourceSnapshot;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.fixture.FakeJobSource;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobRepository;
import io.roleos.job.port.JobRepositories.SourceSnapshotRepository;
import io.roleos.job.port.JobSourcePort.JobSearchCriteria;
import io.roleos.job.port.JobSourcePort.JobSourceContext;
import io.roleos.job.port.JobSourcePort.RawJobDetail;
import io.roleos.job.port.JobSourcePort.RawJobSummary;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Discovery 必须隔离单个详情失败，并保存每个成功岗位的原始快照。 */
class JobDiscoveryServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-19T05:00:00Z");

  @Test
  void persistsSuccessfulDetailsAndKeepsPartialFailuresVisible() {
    RawJobSummary success = summary("success");
    RawJobSummary failed = summary("failed");
    FakeJobSource source =
        new FakeJobSource(List.of(success, failed))
            .detail(
                new RawJobDetail(
                    success,
                    "负责 Java 与 MCP 平台",
                    Map.of("externalJobId", "success", "raw", true),
                    NOW,
                    BrowserProviderType.FAKE))
            .fail("failed", new IllegalStateException("fixture unavailable"));
    MemoryJobs repositories = new MemoryJobs();
    JobDiscoveryService service =
        new JobDiscoveryService(
            source,
            new JobNormalizationService(Clock.fixed(NOW, ZoneOffset.UTC)),
            repositories,
            repositories,
            Clock.fixed(NOW, ZoneOffset.UTC));

    JobDiscoveryService.DiscoveryResult result =
        service.discover(
            UserId.random(),
            new JobSourceContext(UUID.randomUUID(), BrowserProviderType.FAKE, null, null),
            new JobSearchCriteria(
                List.of("AI Agent"),
                "上海",
                20_000,
                3,
                "AI Agent Engineer",
                BrowserProviderType.FAKE));

    assertThat(result.discovered()).isEqualTo(2);
    assertThat(result.savedJobs()).hasSize(1);
    assertThat(result.failures())
        .singleElement()
        .satisfies(f -> assertThat(f.externalJobId()).isEqualTo("failed"));
    assertThat(repositories.snapshots).hasSize(1);
    assertThat(repositories.snapshots.getFirst().rawPayload()).contains("externalJobId=success");
  }

  @Test
  void deduplicatesFiltersAndPreservesExistingSkippedStrategy() {
    RawJobSummary summary = summary("stable");
    FakeJobSource source =
        new FakeJobSource(List.of(summary))
            .detail(
                new RawJobDetail(
                    summary,
                    "负责 Java 与 MCP 平台",
                    Map.of("externalJobId", "stable"),
                    NOW,
                    BrowserProviderType.FAKE));
    MemoryJobs repositories = new MemoryJobs();
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    JobDiscoveryService service =
        new JobDiscoveryService(
            source,
            new JobNormalizationService(clock),
            repositories,
            repositories,
            repositories,
            new JobHardFilterService(clock, "hard-filter-v1"),
            clock);
    UserId userId = UserId.random();
    JobSearchCriteria criteria =
        new JobSearchCriteria(
            List.of("AI Agent"), "上海", 20_000, 5, "AI Agent Engineer", BrowserProviderType.FAKE);

    var first =
        service.discover(
            userId,
            new JobSourceContext(UUID.randomUUID(), BrowserProviderType.FAKE, null, null),
            criteria);
    JobCandidate candidate = repositories.candidates.values().iterator().next();
    repositories.save(
        new JobCandidate(
            candidate.id(),
            candidate.userId(),
            candidate.jobId(),
            candidate.filterResult(),
            candidate.rankingScore(),
            JobStrategy.SKIPPED,
            candidate.strategyRecommendation(),
            candidate.semanticAnalysisStatus(),
            "用户跳过",
            candidate.metadata(),
            candidate.version(),
            candidate.createdAt(),
            NOW));
    var second =
        service.discover(
            userId,
            new JobSourceContext(UUID.randomUUID(), BrowserProviderType.FAKE, null, null),
            criteria);

    assertThat(first.filteredCandidates()).isEqualTo(1);
    assertThat(second.savedJobs())
        .singleElement()
        .extracting(Job::id)
        .isEqualTo(first.savedJobs().getFirst().id());
    assertThat(repositories.jobs).hasSize(1);
    assertThat(repositories.candidates).hasSize(1);
    assertThat(repositories.candidates.values().iterator().next().strategy())
        .isEqualTo(JobStrategy.SKIPPED);
    assertThat(repositories.filters).hasSize(1);
  }

  private RawJobSummary summary(String id) {
    return new RawJobSummary(
        Source.BOSS,
        id,
        "AI Agent 工程师",
        "示例公司",
        "上海",
        "20-30K·14薪",
        "3-5年",
        "本科",
        NOW,
        RecruiterActivity.TODAY,
        URI.create("https://www.zhipin.com/job_detail/" + id + ".html"),
        Map.of());
  }

  private static final class MemoryJobs
      implements JobRepository, SourceSnapshotRepository, JobCandidateRepository {
    private final Map<UUID, Job> jobs = new LinkedHashMap<>();
    private final List<SourceSnapshot> snapshots = new ArrayList<>();
    private final Map<UUID, JobCandidate> candidates = new LinkedHashMap<>();
    private final Map<UUID, FilterEvaluation> filters = new LinkedHashMap<>();

    @Override
    public Optional<Job> findBySourceIdentity(UserId userId, String source, String externalJobId) {
      return jobs.values().stream()
          .filter(job -> job.userId().equals(userId))
          .filter(job -> job.source().name().equals(source))
          .filter(job -> job.externalJobId().equals(externalJobId))
          .findFirst();
    }

    @Override
    public Optional<Job> findByContentIdentity(
        UserId userId,
        String normalizedCompany,
        String normalizedTitle,
        String city,
        String contentHash) {
      return jobs.values().stream()
          .filter(job -> job.userId().equals(userId))
          .filter(job -> job.normalizedCompany().equals(normalizedCompany))
          .filter(job -> job.normalizedTitle().equals(normalizedTitle))
          .filter(job -> java.util.Objects.equals(job.city(), city))
          .filter(job -> job.contentHash().equals(contentHash))
          .findFirst();
    }

    @Override
    public Optional<Job> findById(UserId userId, UUID jobId) {
      return Optional.ofNullable(jobs.get(jobId)).filter(job -> job.userId().equals(userId));
    }

    @Override
    public Job save(Job job) {
      jobs.put(job.id(), job);
      return job;
    }

    @Override
    public Optional<SourceSnapshot> findByJobAndContentHash(UUID jobId, String contentHash) {
      return snapshots.stream()
          .filter(value -> value.jobId().equals(jobId) && value.contentHash().equals(contentHash))
          .findFirst();
    }

    @Override
    public SourceSnapshot save(SourceSnapshot snapshot) {
      snapshots.add(snapshot);
      return snapshot;
    }

    @Override
    public Optional<JobCandidate> find(UserId userId, UUID jobId) {
      return candidates.values().stream()
          .filter(candidate -> candidate.userId().equals(userId))
          .filter(candidate -> candidate.jobId().equals(jobId))
          .findFirst();
    }

    @Override
    public List<JobCandidate> findAll(
        UserId userId, JobStrategy strategy, FilterResult filterResult, int offset, int limit) {
      return candidates.values().stream()
          .filter(candidate -> candidate.userId().equals(userId))
          .filter(candidate -> strategy == null || candidate.strategy() == strategy)
          .filter(candidate -> filterResult == null || candidate.filterResult() == filterResult)
          .skip(offset)
          .limit(limit)
          .toList();
    }

    @Override
    public long count(UserId userId, JobStrategy strategy, FilterResult filterResult) {
      return candidates.values().stream()
          .filter(candidate -> candidate.userId().equals(userId))
          .filter(candidate -> strategy == null || candidate.strategy() == strategy)
          .filter(candidate -> filterResult == null || candidate.filterResult() == filterResult)
          .count();
    }

    @Override
    public JobCandidate save(JobCandidate candidate) {
      candidates.put(candidate.id(), candidate);
      return candidate;
    }

    @Override
    public FilterEvaluation saveFilterEvaluation(FilterEvaluation evaluation) {
      filters.put(evaluation.candidateId(), evaluation);
      return evaluation;
    }

    @Override
    public Optional<FilterEvaluation> findFilterEvaluation(UUID candidateId) {
      return Optional.ofNullable(filters.get(candidateId));
    }
  }
}
