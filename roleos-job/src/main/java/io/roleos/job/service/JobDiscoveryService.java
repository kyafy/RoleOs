package io.roleos.job.service;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobSearch.SourceSnapshot;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.ParseStatus;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobRepository;
import io.roleos.job.port.JobRepositories.SourceSnapshotRepository;
import io.roleos.job.port.JobSourcePort;
import io.roleos.job.port.JobSourcePort.JobSearchCriteria;
import io.roleos.job.port.JobSourcePort.JobSourceContext;
import io.roleos.job.port.JobSourcePort.JobSourceException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 执行 List → Detail → Normalize → Persist，并隔离单岗位来源失败。 */
public final class JobDiscoveryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobDiscoveryService.class);

  private final JobSourcePort source;
  private final JobNormalizationService normalization;
  private final SourceSnapshotRepository snapshots;
  private final Clock clock;
  private final JobDedupService deduplication;
  private final JobCandidateRepository candidates;
  private final JobHardFilterService hardFilter;

  public JobDiscoveryService(
      JobSourcePort source,
      JobNormalizationService normalization,
      JobRepository jobs,
      SourceSnapshotRepository snapshots,
      Clock clock) {
    this.source = Objects.requireNonNull(source);
    this.normalization = Objects.requireNonNull(normalization);
    Objects.requireNonNull(jobs);
    this.snapshots = Objects.requireNonNull(snapshots);
    this.clock = Objects.requireNonNull(clock);
    this.deduplication = new JobDedupService(jobs);
    this.candidates = null;
    this.hardFilter = null;
  }

  /** 创建包含去重、Candidate 和硬筛的完整发现流水线。 */
  public JobDiscoveryService(
      JobSourcePort source,
      JobNormalizationService normalization,
      JobRepository jobs,
      SourceSnapshotRepository snapshots,
      JobCandidateRepository candidates,
      JobHardFilterService hardFilter,
      Clock clock) {
    this.source = Objects.requireNonNull(source);
    this.normalization = Objects.requireNonNull(normalization);
    Objects.requireNonNull(jobs);
    this.snapshots = Objects.requireNonNull(snapshots);
    this.clock = Objects.requireNonNull(clock);
    this.deduplication = new JobDedupService(jobs);
    this.candidates = Objects.requireNonNull(candidates);
    this.hardFilter = Objects.requireNonNull(hardFilter);
  }

  /** 发现岗位；失败列表只包含安全原因码，不暴露上游响应或原始页面。 */
  public DiscoveryResult discover(
      UserId userId, JobSourceContext context, JobSearchCriteria criteria) {
    return discover(userId, context, criteria, ignored -> {});
  }

  /** 每次详情抓取前报告业务恢复点，供编排器先持久化而非保存 Provider 会话。 */
  public DiscoveryResult discover(
      UserId userId,
      JobSourceContext context,
      JobSearchCriteria criteria,
      Consumer<io.roleos.job.port.JobSourcePort.RawJobSummary> checkpoint) {
    UUID searchId = context.searchId();
    long started = System.nanoTime();
    var summaries = source.search(criteria, context);
    Map<UUID, Job> saved = new LinkedHashMap<>();
    List<DiscoveryFailure> failures = new ArrayList<>();
    int filtered = 0;
    io.roleos.job.domain.JobTypes.BrowserProviderType activeProvider = null;

    for (var summary : summaries) {
      try {
        checkpoint.accept(summary);
        var detail = source.detail(summary, context);
        activeProvider = detail.provider();
        Job job = deduplication.resolve(normalization.normalize(userId, detail));
        snapshots.save(
            new SourceSnapshot(
                UUID.randomUUID(),
                job.id(),
                searchId,
                job.source(),
                job.externalJobId(),
                job.sourceUrl(),
                detail.rawData().toString(),
                job.contentHash(),
                detail.provider(),
                detail.fetchedAt(),
                ParseStatus.SUCCEEDED,
                null));
        saved.put(job.id(), job);
        if (candidates != null) {
          processCandidate(userId, criteria, job);
          filtered++;
        }
      } catch (JobSourceException exception) {
        throw exception;
      } catch (RuntimeException exception) {
        failures.add(new DiscoveryFailure(summary.externalJobId(), "JOB_DETAIL_FAILED"));
        LOGGER
            .atWarn()
            .addKeyValue("event", "job.detail.failed")
            .addKeyValue("searchId", searchId)
            .addKeyValue("reasonCode", "JOB_DETAIL_FAILED")
            .log("岗位详情处理失败，已隔离该候选项");
      }
    }
    LOGGER
        .atInfo()
        .addKeyValue("event", "job.discovery.completed")
        .addKeyValue("searchId", searchId)
        .addKeyValue("discoveredCount", summaries.size())
        .addKeyValue("savedCount", saved.size())
        .addKeyValue("failureCount", failures.size())
        .addKeyValue("durationMs", (System.nanoTime() - started) / 1_000_000)
        .log("岗位发现处理已完成");
    return new DiscoveryResult(
        summaries.size(), List.copyOf(saved.values()), filtered, failures, activeProvider);
  }

  private void processCandidate(UserId userId, JobSearchCriteria criteria, Job job) {
    JobCandidate current =
        candidates.find(userId, job.id()).orElseGet(() -> newCandidate(userId, job.id()));
    var filterCriteria =
        new JobHardFilterService.FilterCriteria(
            criteria.city(),
            criteria.salaryMin(),
            criteria.experienceYears(),
            criteria.targetRole());
    var evaluation = hardFilter.evaluate(current.id(), job, filterCriteria);
    JobCandidate updated =
        new JobCandidate(
            current.id(),
            current.userId(),
            current.jobId(),
            evaluation.overallResult(),
            evaluation.overallResult() == FilterResult.REJECT ? null : current.rankingScore(),
            current.strategy(),
            current.strategyRecommendation(),
            current.semanticAnalysisStatus(),
            current.decisionReason(),
            current.metadata(),
            current.version(),
            current.createdAt(),
            clock.instant());
    candidates.save(updated);
    candidates.saveFilterEvaluation(evaluation);
    LOGGER
        .atInfo()
        .addKeyValue("event", "job.filter.completed")
        .addKeyValue("jobId", job.id())
        .addKeyValue("candidateId", current.id())
        .addKeyValue("status", evaluation.overallResult())
        .addKeyValue(
            "reasonCodes",
            List.of(
                evaluation.city().reasonCode(),
                evaluation.salary().reasonCode(),
                evaluation.experience().reasonCode(),
                evaluation.targetRole().reasonCode()))
        .log("岗位硬筛已完成");
  }

  private JobCandidate newCandidate(UserId userId, UUID jobId) {
    var now = clock.instant();
    return new JobCandidate(
        UUID.randomUUID(),
        userId,
        jobId,
        FilterResult.UNKNOWN,
        null,
        JobStrategy.BROAD_APPLY,
        StrategyRecommendation.KEEP_BROAD,
        SemanticAnalysisStatus.PENDING,
        null,
        java.util.Map.of(),
        0,
        now,
        now);
  }

  public record DiscoveryResult(
      int discovered,
      List<Job> savedJobs,
      int filteredCandidates,
      List<DiscoveryFailure> failures,
      io.roleos.job.domain.JobTypes.BrowserProviderType activeProvider) {
    public DiscoveryResult {
      savedJobs = List.copyOf(savedJobs);
      failures = List.copyOf(failures);
    }
  }

  public record DiscoveryFailure(String externalJobId, String reasonCode) {}
}
