package io.roleos.storage.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobCandidate.FilterEvaluation;
import io.roleos.job.domain.JobCandidate.RuleResult;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.storage.career.CareerUserJdbcSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL Candidate、硬筛证据与排序证据适配器。 */
@Repository
@SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
public class JdbcJobCandidateRepositoryAdapter implements JobCandidateRepository {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final CareerUserJdbcSupport users;
  private final ObjectMapper objectMapper;

  @SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
  public JdbcJobCandidateRepositoryAdapter(
      JdbcTemplate jdbc, CareerUserJdbcSupport users, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.users = users;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<JobCandidate> find(UserId userId, UUID jobId) {
    return jdbc
        .query(
            "SELECT * FROM job_candidate WHERE user_id=? AND job_id=?",
            this::map,
            userId.value(),
            jobId)
        .stream()
        .findFirst();
  }

  @Override
  public List<JobCandidate> findAll(
      UserId userId, JobStrategy strategy, FilterResult filterResult, int offset, int limit) {
    if (offset < 0 || limit < 1 || limit > 200) {
      throw new IllegalArgumentException("分页参数超出允许范围");
    }
    return jdbc.query(
        """
        SELECT * FROM job_candidate
        WHERE user_id=?
          AND (CAST(? AS VARCHAR) IS NULL OR strategy=?)
          AND (CAST(? AS VARCHAR) IS NULL OR filter_result=?)
        ORDER BY ranking_score DESC NULLS LAST, updated_at DESC, id
        OFFSET ? LIMIT ?
        """,
        this::map,
        userId.value(),
        name(strategy),
        name(strategy),
        name(filterResult),
        name(filterResult),
        offset,
        limit);
  }

  @Override
  public long count(UserId userId, JobStrategy strategy, FilterResult filterResult) {
    Long total =
        jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM job_candidate
            WHERE user_id=?
              AND (CAST(? AS VARCHAR) IS NULL OR strategy=?)
              AND (CAST(? AS VARCHAR) IS NULL OR filter_result=?)
            """,
            Long.class,
            userId.value(),
            name(strategy),
            name(strategy),
            name(filterResult),
            name(filterResult));
    return total == null ? 0 : total;
  }

  @Override
  @Transactional
  public JobCandidate save(JobCandidate candidate) {
    users.ensure(candidate.userId());
    assertJobOwnership(candidate.userId(), candidate.jobId());
    int changed =
        jdbc.update(
            """
        INSERT INTO job_candidate(
          id,user_id,job_id,filter_result,ranking_score,strategy,strategy_recommendation,
          semantic_analysis_status,decision_reason,metadata_json,version,created_at,updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,CAST(? AS jsonb),?,?,?)
        ON CONFLICT (user_id,job_id) DO UPDATE SET
          filter_result=EXCLUDED.filter_result,ranking_score=EXCLUDED.ranking_score,
          strategy=EXCLUDED.strategy,strategy_recommendation=EXCLUDED.strategy_recommendation,
          semantic_analysis_status=EXCLUDED.semantic_analysis_status,
          decision_reason=EXCLUDED.decision_reason,metadata_json=EXCLUDED.metadata_json,
          version=job_candidate.version+1,updated_at=EXCLUDED.updated_at
        WHERE job_candidate.version=EXCLUDED.version
        """,
            candidate.id(),
            candidate.userId().value(),
            candidate.jobId(),
            candidate.filterResult().name(),
            candidate.rankingScore(),
            candidate.strategy().name(),
            candidate.strategyRecommendation().name(),
            candidate.semanticAnalysisStatus().name(),
            candidate.decisionReason(),
            json(candidate.metadata()),
            candidate.version(),
            Timestamp.from(candidate.createdAt()),
            Timestamp.from(candidate.updatedAt()));
    if (changed == 0) {
      throw new java.util.ConcurrentModificationException("Candidate 版本冲突");
    }
    return find(candidate.userId(), candidate.jobId()).orElseThrow();
  }

  @Override
  public FilterEvaluation saveFilterEvaluation(FilterEvaluation evaluation) {
    jdbc.update(
        """
        INSERT INTO job_filter_evaluation(
          id,candidate_id,overall_result,city_result,salary_result,experience_result,
          target_role_result,rule_version,evaluated_at)
        VALUES (?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),?,?)
        ON CONFLICT (candidate_id) DO UPDATE SET
          overall_result=EXCLUDED.overall_result,city_result=EXCLUDED.city_result,
          salary_result=EXCLUDED.salary_result,experience_result=EXCLUDED.experience_result,
          target_role_result=EXCLUDED.target_role_result,rule_version=EXCLUDED.rule_version,
          evaluated_at=EXCLUDED.evaluated_at
        """,
        evaluation.id(),
        evaluation.candidateId(),
        evaluation.overallResult().name(),
        json(evaluation.city()),
        json(evaluation.salary()),
        json(evaluation.experience()),
        json(evaluation.targetRole()),
        evaluation.ruleVersion(),
        Timestamp.from(evaluation.evaluatedAt()));
    return evaluation;
  }

  @Override
  public Optional<FilterEvaluation> findFilterEvaluation(UUID candidateId) {
    return jdbc
        .query(
            "SELECT * FROM job_filter_evaluation WHERE candidate_id=?",
            (row, number) ->
                new FilterEvaluation(
                    row.getObject("id", UUID.class),
                    row.getObject("candidate_id", UUID.class),
                    FilterResult.valueOf(row.getString("overall_result")),
                    readRule(row.getString("city_result")),
                    readRule(row.getString("salary_result")),
                    readRule(row.getString("experience_result")),
                    readRule(row.getString("target_role_result")),
                    row.getString("rule_version"),
                    row.getTimestamp("evaluated_at").toInstant()),
            candidateId)
        .stream()
        .findFirst();
  }

  private void assertJobOwnership(UserId userId, UUID jobId) {
    Boolean owned =
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM job WHERE id=? AND user_id=?)",
            Boolean.class,
            jobId,
            userId.value());
    if (!Boolean.TRUE.equals(owned)) {
      throw new IllegalArgumentException("岗位不属于当前用户");
    }
  }

  @SuppressWarnings(
      "PMD.UnusedFormalParameter") // JdbcTemplate RowMapper contract supplies row number.
  private JobCandidate map(ResultSet row, int number) throws SQLException {
    return new JobCandidate(
        row.getObject("id", UUID.class),
        new UserId(row.getObject("user_id", UUID.class)),
        row.getObject("job_id", UUID.class),
        FilterResult.valueOf(row.getString("filter_result")),
        row.getBigDecimal("ranking_score"),
        JobStrategy.valueOf(row.getString("strategy")),
        StrategyRecommendation.valueOf(row.getString("strategy_recommendation")),
        SemanticAnalysisStatus.valueOf(row.getString("semantic_analysis_status")),
        row.getString("decision_reason"),
        readMap(row.getString("metadata_json")),
        row.getLong("version"),
        row.getTimestamp("created_at").toInstant(),
        row.getTimestamp("updated_at").toInstant());
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Job Intelligence JSON 无法序列化", exception);
    }
  }

  private Map<String, Object> readMap(String value) {
    try {
      return objectMapper.readValue(value, MAP_TYPE);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Candidate metadata 无法读取", exception);
    }
  }

  private RuleResult readRule(String value) {
    try {
      return objectMapper.readValue(value, RuleResult.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Filter Rule JSON 无法读取", exception);
    }
  }

  private static String name(Enum<?> value) {
    return value == null ? null : value.name();
  }
}
