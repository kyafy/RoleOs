package io.roleos.storage.job;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobSearch.JobDecision;
import io.roleos.job.domain.JobTypes.DecisionType;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.port.JobRepositories.JobDecisionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 在同一事务中保存用户 JobDecision 与 Candidate 有效策略。 */
@Repository
@SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
public class JdbcJobDecisionRepositoryAdapter implements JobDecisionRepository {

  private final JdbcTemplate jdbc;
  private final JdbcJobCandidateRepositoryAdapter candidates;

  @SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
  public JdbcJobDecisionRepositoryAdapter(
      JdbcTemplate jdbc, JdbcJobCandidateRepositoryAdapter candidates) {
    this.jdbc = jdbc;
    this.candidates = candidates;
  }

  @Override
  public Optional<JobDecision> findByCommandId(UserId userId, String commandId) {
    return jdbc
        .query(
            "SELECT * FROM job_decision WHERE user_id=? AND command_id=?",
            this::map,
            userId.value(),
            commandId)
        .stream()
        .findFirst();
  }

  @Override
  @Transactional
  public JobDecision save(JobDecision decision, JobCandidate resultingCandidate) {
    try {
      candidates.save(resultingCandidate);
      jdbc.update(
          """
          INSERT INTO job_decision(
            id,candidate_id,user_id,command_id,decision,previous_strategy,resulting_strategy,
            reason,decided_at)
          VALUES (?,?,?,?,?,?,?,?,?)
          """,
          decision.id(),
          decision.candidateId(),
          decision.userId().value(),
          decision.commandId(),
          decision.decision().name(),
          decision.previousStrategy().name(),
          decision.resultingStrategy().name(),
          decision.reason(),
          Timestamp.from(decision.decidedAt()));
      return decision;
    } catch (DuplicateKeyException exception) {
      throw new ConcurrentModificationException("岗位决策幂等键冲突", exception);
    }
  }

  @SuppressWarnings(
      "PMD.UnusedFormalParameter") // JdbcTemplate RowMapper contract supplies row number.
  private JobDecision map(ResultSet row, int number) throws SQLException {
    return new JobDecision(
        row.getObject("id", java.util.UUID.class),
        row.getObject("candidate_id", java.util.UUID.class),
        new UserId(row.getObject("user_id", java.util.UUID.class)),
        row.getString("command_id"),
        DecisionType.valueOf(row.getString("decision")),
        JobStrategy.valueOf(row.getString("previous_strategy")),
        JobStrategy.valueOf(row.getString("resulting_strategy")),
        row.getString("reason"),
        row.getTimestamp("decided_at").toInstant());
  }
}
