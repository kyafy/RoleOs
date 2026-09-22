package io.roleos.storage.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobCandidate.FilterEvaluation;
import io.roleos.job.domain.JobCandidate.RuleResult;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.storage.PostgreSqlIntegrationSupport;
import io.roleos.storage.career.CareerUserJdbcSupport;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Candidate 与 FilterEvaluation 的 PostgreSQL 约束和用户隔离验证。 */
class JdbcJobCandidateRepositoryAdapterIT extends PostgreSqlIntegrationSupport {

  private static final Instant NOW = Instant.parse("2026-09-19T06:00:00Z");

  private JdbcTemplate jdbc;
  private JdbcJobCandidateRepositoryAdapter candidates;
  private JdbcJobRepositoryAdapter jobs;

  @BeforeEach
  void setUp() {
    DataSource dataSource = migrateSchema().getConfiguration().getDataSource();
    jdbc = new JdbcTemplate(dataSource);
    CareerUserJdbcSupport users = new CareerUserJdbcSupport(jdbc);
    ObjectMapper objectMapper = new ObjectMapper();
    candidates = new JdbcJobCandidateRepositoryAdapter(jdbc, users, objectMapper);
    jobs = new JdbcJobRepositoryAdapter(jdbc, users, objectMapper);
  }

  @Test
  void upsertsSingleCandidatePerUserAndJobAndUpdatesFilterEvidence() {
    UserId userId = UserId.random();
    Job job = jobs.save(job(userId));
    JobCandidate original = candidate(userId, job.id(), FilterResult.UNKNOWN);

    JobCandidate first = candidates.save(original);
    JobCandidate updated =
        candidates.save(
            new JobCandidate(
                UUID.randomUUID(),
                userId,
                job.id(),
                FilterResult.PASS,
                null,
                JobStrategy.BROAD_APPLY,
                StrategyRecommendation.KEEP_BROAD,
                SemanticAnalysisStatus.PENDING,
                null,
                Map.of("source", "filter"),
                first.version(),
                first.createdAt(),
                NOW.plusSeconds(1)));
    FilterEvaluation evaluation = filter(first.id(), FilterResult.PASS, "hard-filter-v2");
    candidates.saveFilterEvaluation(filter(first.id(), FilterResult.UNKNOWN, "hard-filter-v1"));
    candidates.saveFilterEvaluation(evaluation);

    assertThat(updated.id()).isEqualTo(first.id());
    assertThat(updated.filterResult()).isEqualTo(FilterResult.PASS);
    assertThat(updated.strategy()).isEqualTo(JobStrategy.BROAD_APPLY);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM job_candidate WHERE user_id=? AND job_id=?",
                Integer.class,
                userId.value(),
                job.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT rule_version FROM job_filter_evaluation WHERE candidate_id=?",
                String.class,
                first.id()))
        .isEqualTo("hard-filter-v2");
  }

  @Test
  void preventsCrossUserReadsAndOwnershipMismatchWrites() {
    UserId owner = UserId.random();
    UserId other = UserId.random();
    Job job = jobs.save(job(owner));
    candidates.save(candidate(owner, job.id(), FilterResult.PASS));

    assertThat(candidates.find(owner, job.id())).isPresent();
    assertThat(candidates.find(other, job.id())).isEmpty();
    assertThatThrownBy(() -> candidates.save(candidate(other, job.id(), FilterResult.PASS)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("不属于当前用户");
  }

  private JobCandidate candidate(UserId userId, UUID jobId, FilterResult result) {
    return new JobCandidate(
        UUID.randomUUID(),
        userId,
        jobId,
        result,
        null,
        JobStrategy.BROAD_APPLY,
        StrategyRecommendation.KEEP_BROAD,
        SemanticAnalysisStatus.PENDING,
        null,
        Map.of(),
        0,
        NOW,
        NOW);
  }

  private FilterEvaluation filter(UUID candidateId, FilterResult result, String version) {
    RuleResult rule = new RuleResult(result, "FIXTURE", "fixture", Map.of("known", true));
    return new FilterEvaluation(
        UUID.randomUUID(), candidateId, result, rule, rule, rule, rule, version, NOW);
  }

  private Job job(UserId userId) {
    String externalId = UUID.randomUUID().toString();
    return new Job(
        UUID.randomUUID(),
        userId,
        Source.BOSS,
        externalId,
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
        "负责 AI Agent 平台",
        "负责 AI Agent 平台",
        NOW,
        RecruiterActivity.TODAY,
        URI.create("https://www.zhipin.com/job_detail/" + externalId + ".html"),
        SourceStatus.ACTIVE,
        "e".repeat(64),
        Map.of(),
        NOW,
        NOW,
        NOW,
        NOW);
  }
}
