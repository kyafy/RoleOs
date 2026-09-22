package io.roleos.storage.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobSearch.JobDecision;
import io.roleos.job.domain.JobTypes.DecisionType;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.service.JobStrategyService;
import io.roleos.storage.PostgreSqlIntegrationSupport;
import io.roleos.storage.career.CareerUserJdbcSupport;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Decision 与有效策略必须原子提交，乐观锁冲突时不得留下审计记录。 */
class JdbcJobDecisionRepositoryAdapterIT extends PostgreSqlIntegrationSupport {

  private static final Instant NOW = Instant.parse("2026-09-19T08:00:00Z");

  private JdbcTemplate jdbc;
  private JdbcJobCandidateRepositoryAdapter candidates;
  private JdbcJobDecisionRepositoryAdapter decisions;
  private UserId userId;
  private UUID jobId;

  @BeforeEach
  void setUp() {
    DataSource dataSource = migrateSchema().getConfiguration().getDataSource();
    jdbc = new JdbcTemplate(dataSource);
    CareerUserJdbcSupport users = new CareerUserJdbcSupport(jdbc);
    ObjectMapper mapper = new ObjectMapper();
    JdbcJobRepositoryAdapter jobs = new JdbcJobRepositoryAdapter(jdbc, users, mapper);
    candidates = new JdbcJobCandidateRepositoryAdapter(jdbc, users, mapper);
    decisions = new JdbcJobDecisionRepositoryAdapter(jdbc, candidates);
    userId = UserId.random();
    Job job = jobs.save(job(userId));
    jobId = job.id();
    candidates.save(candidate(userId, jobId));
  }

  @Test
  void commitsDecisionAndStrategyTogetherAndReplaysIdempotently() {
    JobStrategyService service =
        new JobStrategyService(candidates, decisions, Clock.fixed(NOW, ZoneOffset.UTC));

    var first = service.decide(userId, jobId, "command-0001", DecisionType.PROMOTE, "重点岗位");
    var replay = service.decide(userId, jobId, "command-0001", DecisionType.PROMOTE, "重点岗位");

    assertThat(first.candidate().strategy()).isEqualTo(JobStrategy.TARGETED_APPLY);
    assertThat(replay.decision().id()).isEqualTo(first.decision().id());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM job_decision", Integer.class))
        .isEqualTo(1);
    assertThat(candidates.find(userId, jobId).orElseThrow().strategy())
        .isEqualTo(JobStrategy.TARGETED_APPLY);
  }

  @Test
  void optimisticLockConflictRollsBackDecisionInsert() {
    JobCandidate current = candidates.find(userId, jobId).orElseThrow();
    JobCandidate firstUpdate = withStrategy(current, JobStrategy.TARGETED_APPLY, current.version());
    candidates.save(firstUpdate);
    JobCandidate stale = withStrategy(current, JobStrategy.SKIPPED, current.version());
    JobDecision decision =
        new JobDecision(
            UUID.randomUUID(),
            current.id(),
            userId,
            "command-0002",
            DecisionType.SKIP,
            current.strategy(),
            JobStrategy.SKIPPED,
            null,
            NOW);

    assertThatThrownBy(() -> decisions.save(decision, stale))
        .isInstanceOf(java.util.ConcurrentModificationException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM job_decision", Integer.class)).isZero();
    assertThat(candidates.find(userId, jobId).orElseThrow().strategy())
        .isEqualTo(JobStrategy.TARGETED_APPLY);
  }

  private JobCandidate withStrategy(JobCandidate value, JobStrategy strategy, long version) {
    return new JobCandidate(
        value.id(),
        value.userId(),
        value.jobId(),
        value.filterResult(),
        value.rankingScore(),
        strategy,
        value.strategyRecommendation(),
        value.semanticAnalysisStatus(),
        value.decisionReason(),
        value.metadata(),
        version,
        value.createdAt(),
        NOW.plusSeconds(1));
  }

  private JobCandidate candidate(UserId owner, UUID id) {
    return new JobCandidate(
        UUID.randomUUID(),
        owner,
        id,
        FilterResult.PASS,
        null,
        JobStrategy.BROAD_APPLY,
        StrategyRecommendation.PROMOTE_TO_TARGETED,
        SemanticAnalysisStatus.SUCCEEDED,
        null,
        Map.of(),
        0,
        NOW,
        NOW);
  }

  private Job job(UserId owner) {
    String id = UUID.randomUUID().toString();
    return new Job(
        UUID.randomUUID(),
        owner,
        Source.BOSS,
        id,
        "AI Agent 工程师",
        "ai agent 工程师",
        "RoleOS",
        "roleos",
        "上海",
        20_000,
        30_000,
        14,
        3,
        5,
        "本科",
        "负责 Agent 平台",
        "负责 Agent 平台",
        NOW,
        RecruiterActivity.TODAY,
        URI.create("https://www.zhipin.com/job_detail/" + id + ".html"),
        SourceStatus.ACTIVE,
        "b".repeat(64),
        Map.of(),
        NOW,
        NOW,
        NOW,
        NOW);
  }
}
