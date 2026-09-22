package io.roleos.job.service;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobSearch;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.SearchState;
import io.roleos.job.domain.JobTypes.SearchStep;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobSearchRepository;
import io.roleos.job.port.JobRepositories.RankingEvaluationRepository;
import io.roleos.job.port.JobSourcePort.JobSearchCriteria;
import io.roleos.job.port.JobSourcePort.JobSourceContext;
import io.roleos.job.port.JobSourcePort.JobSourceException;
import io.roleos.job.port.JobTelemetry;
import io.roleos.job.port.SemanticRankingPort.RankingContext;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 持久化搜索业务状态并串联发现、硬筛与 Broad Ranking。 */
public final class JobSearchOrchestrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobSearchOrchestrator.class);
  private static final String LOG_COMMAND_ID = "commandId";
  private static final String LOG_EVENT = "event";
  private static final String LOG_SEARCH_ID = "searchId";
  private static final String LOG_STATUS = "status";

  private final JobSearchRepository searches;
  private final JobCandidateRepository candidates;
  private final RankingEvaluationRepository rankings;
  private final JobDiscoveryService discovery;
  private final BroadRankingService ranking;
  private final Clock clock;
  private final JobTelemetry telemetry;

  public JobSearchOrchestrator(
      JobSearchRepository searches,
      JobCandidateRepository candidates,
      RankingEvaluationRepository rankings,
      JobDiscoveryService discovery,
      BroadRankingService ranking,
      Clock clock) {
    this(searches, candidates, rankings, discovery, ranking, clock, JobTelemetry.NOOP);
  }

  public JobSearchOrchestrator(
      JobSearchRepository searches,
      JobCandidateRepository candidates,
      RankingEvaluationRepository rankings,
      JobDiscoveryService discovery,
      BroadRankingService ranking,
      Clock clock,
      JobTelemetry telemetry) {
    this.searches = Objects.requireNonNull(searches);
    this.candidates = Objects.requireNonNull(candidates);
    this.rankings = Objects.requireNonNull(rankings);
    this.discovery = Objects.requireNonNull(discovery);
    this.ranking = Objects.requireNonNull(ranking);
    this.clock = Objects.requireNonNull(clock);
    this.telemetry = Objects.requireNonNull(telemetry);
  }

  /** 同步执行一次低频搜索；外层 API 可在后续替换为任务执行器而不改变状态机。 */
  public JobSearch start(UserId userId, JobSearchCriteria criteria, RankingContext context) {
    return start(userId, UUID.randomUUID().toString(), criteria, context);
  }

  /** 同一用户的相同开始命令只会创建并执行一次搜索。 */
  public JobSearch start(
      UserId userId, String commandId, JobSearchCriteria criteria, RankingContext context) {
    Objects.requireNonNull(userId);
    requireCommandId(commandId);
    Objects.requireNonNull(criteria);
    Objects.requireNonNull(context);
    var existing = searches.findByStartCommandId(userId, commandId);
    if (existing.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "job.search.idempotency_hit")
          .addKeyValue(LOG_SEARCH_ID, existing.get().id())
          .addKeyValue(LOG_COMMAND_ID, commandId)
          .log("岗位搜索开始命令已幂等命中");
      return existing.get();
    }
    var now = clock.instant();
    UUID searchId = UUID.randomUUID();
    URI resumeUrl = URI.create("https://www.zhipin.com/web/geek/job");
    Map<String, Object> savedCriteria = new LinkedHashMap<>();
    savedCriteria.put("keywords", criteria.keywords());
    savedCriteria.put("city", criteria.city());
    if (criteria.salaryMin() != null) savedCriteria.put("salaryMin", criteria.salaryMin());
    if (criteria.experienceYears() != null) {
      savedCriteria.put("experienceYears", criteria.experienceYears());
    }
    savedCriteria.put("targetRole", criteria.targetRole());
    savedCriteria.put("rankingSkills", context.skills());
    if (context.careerGoal() != null) savedCriteria.put("careerGoal", context.careerGoal());
    JobSearch search =
        new JobSearch(
            searchId,
            userId,
            savedCriteria,
            commandId,
            null,
            criteria.provider(),
            null,
            SearchState.CREATED,
            SearchStep.NAVIGATE_SEARCH,
            resumeUrl,
            null,
            null,
            null,
            0,
            JobSearch.Counts.ZERO,
            now,
            now,
            null);
    try {
      search = searches.save(search);
    } catch (RuntimeException exception) {
      var concurrent = searches.findByStartCommandId(userId, commandId);
      if (concurrent.isPresent()) return concurrent.get();
      throw exception;
    }
    search = searches.save(search.transitionTo(SearchState.RUNNING, now, false));
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "job.search.started")
        .addKeyValue(LOG_SEARCH_ID, search.id())
        .addKeyValue("provider", criteria.provider())
        .addKeyValue(LOG_STATUS, "RUNNING")
        .addKeyValue(LOG_COMMAND_ID, commandId)
        .log("岗位搜索已开始");
    telemetry.record(search.id(), "search", "started", criteria.provider(), Duration.ZERO, null);
    return execute(search, criteria, context);
  }

  private JobSearch execute(JobSearch search, JobSearchCriteria criteria, RankingContext context) {
    Instant executionStarted = clock.instant();
    JobSearch current = search;
    try {
      current =
          searches.save(
              current.advance(
                  SearchStep.DETAIL_FETCH,
                  criteria.provider(),
                  current.resumeUrl(),
                  current.cursor(),
                  clock.instant()));
      AtomicReference<JobSearch> detailProgress = new AtomicReference<>(current);
      var result =
          discovery.discover(
              current.userId(),
              new JobSourceContext(
                  current.id(), current.requestedProvider(), current.resumeUrl(), current.cursor()),
              criteria,
              summary -> {
                JobSearch pending = detailProgress.get();
                detailProgress.set(
                    searches.save(
                        pending.advance(
                            SearchStep.DETAIL_FETCH,
                            pending.activeProvider() == null
                                ? criteria.provider()
                                : pending.activeProvider(),
                            summary.sourceUrl(),
                            summary.externalJobId(),
                            clock.instant())));
              });
      current = detailProgress.get();
      int normalized = result.savedJobs().size();
      UserId searchUserId = current.userId();
      int rejected =
          (int)
              result.savedJobs().stream()
                  .map(job -> candidates.find(searchUserId, job.id()).orElseThrow())
                  .filter(candidate -> candidate.filterResult() == FilterResult.REJECT)
                  .count();
      current =
          searches.save(
              current.withCounts(
                  new JobSearch.Counts(result.discovered(), normalized, rejected, 0),
                  clock.instant()));
      current =
          searches.save(
              current.advance(
                  SearchStep.RANK,
                  result.activeProvider() == null ? criteria.provider() : result.activeProvider(),
                  current.resumeUrl(),
                  current.cursor(),
                  clock.instant()));
      int ranked = 0;
      for (var job : result.savedJobs()) {
        JobCandidate candidate = candidates.find(current.userId(), job.id()).orElseThrow();
        if (candidate.filterResult() == FilterResult.REJECT) continue;
        var evaluation = ranking.rank(candidate.id(), job, candidate.filterResult(), context);
        rankings.save(evaluation);
        candidates.save(
            new JobCandidate(
                candidate.id(),
                candidate.userId(),
                candidate.jobId(),
                candidate.filterResult(),
                evaluation.totalScore(),
                candidate.strategy(),
                evaluation.recommendation(),
                evaluation.semanticScore() == null
                    ? SemanticAnalysisStatus.FAILED
                    : SemanticAnalysisStatus.SUCCEEDED,
                candidate.decisionReason(),
                candidate.metadata(),
                candidate.version(),
                candidate.createdAt(),
                clock.instant()));
        ranked++;
        current =
            searches.save(
                current.withCounts(
                    new JobSearch.Counts(result.discovered(), normalized, rejected, ranked),
                    clock.instant()));
      }
      JobSearch completed =
          searches.save(current.transitionTo(SearchState.COMPLETED, clock.instant(), false));
      Duration duration = Duration.between(executionStarted, completed.updatedAt());
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "job.search.completed")
          .addKeyValue(LOG_SEARCH_ID, completed.id())
          .addKeyValue("provider", completed.activeProvider())
          .addKeyValue("durationMs", duration.toMillis())
          .addKeyValue(LOG_STATUS, "COMPLETED")
          .log("岗位搜索已完成");
      telemetry.record(
          completed.id(), "search", "completed", completed.activeProvider(), duration, null);
      return completed;
    } catch (JobSourceException exception) {
      if (exception.kind() == io.roleos.job.port.JobSourcePort.FailureKind.HUMAN_REQUIRED) {
        JobSearch paused =
            searches.save(current.pauseForHuman(exception.reasonCode(), clock.instant()));
        recordFailure(paused, "human_pause", exception.reasonCode(), executionStarted);
        return paused;
      }
      JobSearch failed =
          searches.save(
              current.fail(
                  exception.reasonCode(),
                  exception.kind() == io.roleos.job.port.JobSourcePort.FailureKind.RETRYABLE,
                  clock.instant()));
      recordFailure(failed, "failure", exception.reasonCode(), executionStarted);
      return failed;
    } catch (RuntimeException exception) {
      JobSearch failed = searches.save(current.fail("JOB_SEARCH_FAILED", true, clock.instant()));
      recordFailure(failed, "failure", "JOB_SEARCH_FAILED", executionStarted);
      return failed;
    }
  }

  private void recordFailure(
      JobSearch search, String event, String reasonCode, Instant executionStarted) {
    Duration duration = Duration.between(executionStarted, search.updatedAt());
    LOGGER
        .atWarn()
        .addKeyValue(LOG_EVENT, "job.search." + event)
        .addKeyValue(LOG_SEARCH_ID, search.id())
        .addKeyValue("provider", search.activeProvider())
        .addKeyValue("durationMs", duration.toMillis())
        .addKeyValue(LOG_STATUS, search.state())
        .addKeyValue("reasonCode", reasonCode)
        .log("岗位搜索未完成，已进入受控状态");
    telemetry.record(
        search.id(), event, search.state().name(), search.activeProvider(), duration, reasonCode);
  }

  public JobSearch find(UserId userId, UUID searchId) {
    return searches
        .find(userId, searchId)
        .orElseThrow(() -> new JobSearchException(FailureReason.NOT_FOUND, "岗位搜索不存在"));
  }

  public JobSearch resume(UserId userId, UUID searchId) {
    return resume(userId, searchId, UUID.randomUUID().toString());
  }

  /** 同一恢复命令只会恢复并执行一次指定的搜索。 */
  public JobSearch resume(UserId userId, UUID searchId, String commandId) {
    requireCommandId(commandId);
    var existing = searches.findByResumeCommandId(userId, commandId);
    if (existing.isPresent()) {
      if (!existing.get().id().equals(searchId)) {
        throw new JobSearchException(FailureReason.CONFLICT, "恢复命令已用于另一条岗位搜索");
      }
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "job.search.resume.idempotency_hit")
          .addKeyValue(LOG_SEARCH_ID, searchId)
          .addKeyValue(LOG_COMMAND_ID, commandId)
          .log("岗位搜索恢复命令已幂等命中");
      return existing.get();
    }
    JobSearch search = find(userId, searchId);
    if (search.state() != SearchState.PAUSED_FOR_HUMAN
        && search.state() != SearchState.RETRYABLE_FAILED) {
      throw new JobSearchException(FailureReason.CONFLICT, "当前搜索状态不可恢复");
    }
    JobSearch commandRecorded = searches.save(search.withResumeCommand(commandId, clock.instant()));
    JobSearch running =
        searches.save(commandRecorded.transitionTo(SearchState.RUNNING, clock.instant(), true));
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "job.search.resumed")
        .addKeyValue(LOG_SEARCH_ID, running.id())
        .addKeyValue("fromStatus", search.state())
        .addKeyValue(LOG_STATUS, running.state())
        .addKeyValue(LOG_COMMAND_ID, commandId)
        .log("岗位搜索已恢复执行");
    return execute(running, criteria(search), context(search));
  }

  private JobSearchCriteria criteria(JobSearch search) {
    Object keywordsValue = search.criteria().get("keywords");
    java.util.List<String> keywords =
        keywordsValue instanceof java.util.List<?> values
            ? values.stream().map(String::valueOf).toList()
            : java.util.List.of();
    return new JobSearchCriteria(
        keywords,
        String.valueOf(search.criteria().get("city")),
        integer(search.criteria().get("salaryMin")),
        integer(search.criteria().get("experienceYears")),
        String.valueOf(search.criteria().get("targetRole")),
        search.requestedProvider());
  }

  private RankingContext context(JobSearch search) {
    Object skillsValue = search.criteria().get("rankingSkills");
    java.util.List<String> skills =
        skillsValue instanceof java.util.List<?> values
            ? values.stream().map(String::valueOf).toList()
            : java.util.List.of();
    Object goalValue = search.criteria().get("careerGoal");
    return new RankingContext(
        String.valueOf(search.criteria().get("targetRole")),
        skills,
        goalValue == null ? null : String.valueOf(goalValue));
  }

  private static Integer integer(Object value) {
    return value instanceof Number number ? number.intValue() : null;
  }

  private static void requireCommandId(String commandId) {
    if (commandId == null || commandId.isBlank()) {
      throw new IllegalArgumentException("幂等命令标识不能为空");
    }
  }

  public enum FailureReason {
    NOT_FOUND,
    CONFLICT
  }

  public static final class JobSearchException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final FailureReason failureReason;

    public JobSearchException(FailureReason reason, String message) {
      super(message);
      this.failureReason = Objects.requireNonNull(reason);
    }

    public FailureReason reason() {
      return failureReason;
    }
  }
}
