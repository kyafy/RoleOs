package io.roleos.storage.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.job.domain.JobCandidate.RankingEvaluation;
import io.roleos.job.port.JobRepositories.RankingEvaluationRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL 当前 RankingEvaluation 适配器，保留规则与语义信号及 Agent Trace。 */
@Repository
@SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
public class JdbcRankingEvaluationRepositoryAdapter implements RankingEvaluationRepository {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<Map<String, BigDecimal>> DECIMAL_MAP =
      new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  @SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
  public JdbcRankingEvaluationRepositoryAdapter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc);
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  @Override
  public RankingEvaluation save(RankingEvaluation evaluation) {
    jdbc.update(
        """
        INSERT INTO job_ranking_evaluation(
          id,candidate_id,rule_score,semantic_score,total_score,recommendation,reasons_json,
          important_signals_json,warnings_json,rule_signals_json,semantic_signals_json,
          agent_trace_id,scoring_version,evaluated_at)
        VALUES (?,?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),
          CAST(? AS jsonb),?,?,?)
        ON CONFLICT (candidate_id) DO UPDATE SET
          rule_score=EXCLUDED.rule_score,semantic_score=EXCLUDED.semantic_score,
          total_score=EXCLUDED.total_score,recommendation=EXCLUDED.recommendation,
          reasons_json=EXCLUDED.reasons_json,important_signals_json=EXCLUDED.important_signals_json,
          warnings_json=EXCLUDED.warnings_json,rule_signals_json=EXCLUDED.rule_signals_json,
          semantic_signals_json=EXCLUDED.semantic_signals_json,
          agent_trace_id=EXCLUDED.agent_trace_id,scoring_version=EXCLUDED.scoring_version,
          evaluated_at=EXCLUDED.evaluated_at
        """,
        evaluation.id(),
        evaluation.candidateId(),
        evaluation.ruleScore(),
        evaluation.semanticScore(),
        evaluation.totalScore(),
        evaluation.recommendation().name(),
        json(evaluation.reasons()),
        json(evaluation.importantSignals()),
        json(evaluation.warnings()),
        json(evaluation.ruleSignals()),
        json(evaluation.semanticSignals()),
        evaluation.agentTraceId(),
        evaluation.scoringVersion(),
        Timestamp.from(evaluation.evaluatedAt()));
    return evaluation;
  }

  @Override
  public Optional<RankingEvaluation> find(UUID candidateId) {
    return jdbc
        .query("SELECT * FROM job_ranking_evaluation WHERE candidate_id=?", this::map, candidateId)
        .stream()
        .findFirst();
  }

  @SuppressWarnings(
      "PMD.UnusedFormalParameter") // JdbcTemplate RowMapper contract supplies row number.
  private RankingEvaluation map(ResultSet row, int number) throws SQLException {
    return new RankingEvaluation(
        row.getObject("id", UUID.class),
        row.getObject("candidate_id", UUID.class),
        row.getBigDecimal("rule_score"),
        row.getBigDecimal("semantic_score"),
        row.getBigDecimal("total_score"),
        io.roleos.job.domain.JobTypes.StrategyRecommendation.valueOf(
            row.getString("recommendation")),
        read(row.getString("reasons_json"), STRING_LIST),
        read(row.getString("important_signals_json"), STRING_LIST),
        read(row.getString("warnings_json"), STRING_LIST),
        read(row.getString("rule_signals_json"), DECIMAL_MAP),
        read(row.getString("semantic_signals_json"), DECIMAL_MAP),
        row.getString("agent_trace_id"),
        row.getString("scoring_version"),
        row.getTimestamp("evaluated_at").toInstant());
  }

  private <T> T read(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("RankingEvaluation JSON 无法读取", exception);
    }
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("RankingEvaluation JSON 无法序列化", exception);
    }
  }
}
