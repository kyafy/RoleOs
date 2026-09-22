package io.roleos.storage.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobCandidate.RankingEvaluation;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.storage.PostgreSqlIntegrationSupport;
import io.roleos.storage.career.CareerUserJdbcSupport;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Ranking 当前结果、降级信号与 Agent Trace 的 PostgreSQL 验证。 */
class JdbcRankingEvaluationRepositoryAdapterIT extends PostgreSqlIntegrationSupport {

  private static final Instant NOW = Instant.parse("2026-09-19T07:00:00Z");

  private JdbcTemplate jdbc;
  private JdbcJobCandidateRepositoryAdapter candidates;
  private JdbcRankingEvaluationRepositoryAdapter rankings;
  private UUID candidateId;

  @BeforeEach
  void setUp() {
    DataSource dataSource = migrateSchema().getConfiguration().getDataSource();
    jdbc = new JdbcTemplate(dataSource);
    ObjectMapper mapper = new ObjectMapper();
    CareerUserJdbcSupport users = new CareerUserJdbcSupport(jdbc);
    JdbcJobRepositoryAdapter jobs = new JdbcJobRepositoryAdapter(jdbc, users, mapper);
    candidates = new JdbcJobCandidateRepositoryAdapter(jdbc, users, mapper);
    rankings = new JdbcRankingEvaluationRepositoryAdapter(jdbc, mapper);
    UserId userId = UserId.random();
    Job job = jobs.save(job(userId));
    candidateId = candidates.save(candidate(userId, job.id())).id();
  }

  @Test
  void updatesCurrentRankingAndRetainsRuleBaselineWhenSemanticFails() {
    rankings.save(
        evaluation(new BigDecimal("82"), "trace-1", Map.of("roleFit", new BigDecimal("90"))));
    candidates.save(withStatus(SemanticAnalysisStatus.FAILED));
    rankings.save(evaluation(null, null, Map.of()));

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM job_ranking_evaluation WHERE candidate_id=?",
                Integer.class,
                candidateId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT semantic_score FROM job_ranking_evaluation WHERE candidate_id=?",
                BigDecimal.class,
                candidateId))
        .isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT rule_signals_json->>'factCompleteness' FROM job_ranking_evaluation WHERE candidate_id=?",
                String.class,
                candidateId))
        .isEqualTo("95");
    assertThat(
            jdbc.queryForObject(
                "SELECT semantic_analysis_status FROM job_candidate WHERE id=?",
                String.class,
                candidateId))
        .isEqualTo("FAILED");
  }

  private RankingEvaluation evaluation(
      BigDecimal semantic, String trace, Map<String, BigDecimal> semanticSignals) {
    return new RankingEvaluation(
        UUID.randomUUID(),
        candidateId,
        new BigDecimal("95"),
        semantic,
        semantic == null ? new BigDecimal("95") : new BigDecimal("87"),
        semantic == null
            ? StrategyRecommendation.REVIEW
            : StrategyRecommendation.PROMOTE_TO_TARGETED,
        List.of("可解释原因"),
        List.of("规则基线"),
        semantic == null ? List.of("语义分析失败") : List.of(),
        Map.of("factCompleteness", new BigDecimal("95")),
        semanticSignals,
        trace,
        "ranking-v1",
        NOW);
  }

  private JobCandidate withStatus(SemanticAnalysisStatus status) {
    JobCandidate current =
        jdbc.query(
                "SELECT user_id,job_id FROM job_candidate WHERE id=?",
                (row, number) ->
                    candidate(
                        new UserId(row.getObject(1, UUID.class)), row.getObject(2, UUID.class)),
                candidateId)
            .getFirst();
    return new JobCandidate(
        current.id(),
        current.userId(),
        current.jobId(),
        current.filterResult(),
        current.rankingScore(),
        current.strategy(),
        current.strategyRecommendation(),
        status,
        current.decisionReason(),
        current.metadata(),
        current.version(),
        current.createdAt(),
        NOW.plusSeconds(1));
  }

  private JobCandidate candidate(UserId userId, UUID jobId) {
    return new JobCandidate(
        candidateId == null ? UUID.randomUUID() : candidateId,
        userId,
        jobId,
        FilterResult.PASS,
        null,
        JobStrategy.BROAD_APPLY,
        StrategyRecommendation.KEEP_BROAD,
        SemanticAnalysisStatus.SUCCEEDED,
        null,
        Map.of(),
        0,
        NOW,
        NOW);
  }

  private Job job(UserId userId) {
    String id = UUID.randomUUID().toString();
    return new Job(
        UUID.randomUUID(),
        userId,
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
        "a".repeat(64),
        Map.of(),
        NOW,
        NOW,
        NOW,
        NOW);
  }
}
