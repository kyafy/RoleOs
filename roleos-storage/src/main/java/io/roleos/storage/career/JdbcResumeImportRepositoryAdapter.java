package io.roleos.storage.career;

import io.roleos.domain.career.UserId;
import io.roleos.domain.career.importing.CandidateDecision;
import io.roleos.domain.career.importing.CandidateReviewStatus;
import io.roleos.domain.career.importing.FactCandidate;
import io.roleos.domain.career.importing.ResumeImport;
import io.roleos.domain.career.importing.ResumeImportRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL adapter for controlled imports; raw resumes never pass through this adapter. */
@Repository
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class JdbcResumeImportRepositoryAdapter implements ResumeImportRepository {
  private final JdbcTemplate jdbcTemplate;
  private final CareerUserJdbcSupport users;

  public JdbcResumeImportRepositoryAdapter(JdbcTemplate jdbcTemplate, CareerUserJdbcSupport users) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    this.users = Objects.requireNonNull(users);
  }

  @Override
  @Transactional
  public ResumeImport save(ResumeImport resumeImport) {
    users.ensure(resumeImport.userId());
    jdbcTemplate.update(
        "INSERT INTO resume_imports(id,user_id,controlled_source_reference,created_at,updated_at,version) VALUES (?,?,?,?,?,0)",
        resumeImport.id(),
        resumeImport.userId().value(),
        resumeImport.controlledSourceReference(),
        Timestamp.from(resumeImport.createdAt()),
        Timestamp.from(resumeImport.createdAt()));
    for (FactCandidate candidate : resumeImport.candidates()) {
      jdbcTemplate.update(
          "INSERT INTO fact_candidates(id,import_id,candidate_type,payload,source_location,review_status,created_at,updated_at,version) VALUES (?,?,?,CAST(? AS JSONB),?,?,?, ?,0)",
          candidate.id(),
          resumeImport.id(),
          candidate.candidateType(),
          candidate.payload(),
          candidate.sourceLocation(),
          candidate.status().name(),
          Timestamp.from(resumeImport.createdAt()),
          Timestamp.from(resumeImport.createdAt()));
    }
    return resumeImport;
  }

  @Override
  public Optional<FactCandidate> findCandidate(UserId userId, UUID candidateId) {
    List<FactCandidate> candidates =
        jdbcTemplate.query(
            "SELECT c.id,c.candidate_type,c.payload::text,c.source_location,c.review_status FROM fact_candidates c JOIN resume_imports i ON c.import_id=i.id WHERE c.id=? AND i.user_id=?",
            (row, number) ->
                FactCandidate.restore(
                    row.getObject("id", UUID.class),
                    row.getString("candidate_type"),
                    row.getString("payload"),
                    row.getString("source_location"),
                    CandidateReviewStatus.valueOf(row.getString("review_status"))),
            candidateId,
            userId.value());
    return candidates.stream().findFirst();
  }

  @Override
  public Optional<ResumeImport> findImport(UserId userId, UUID importId) {
    List<ResumeImport> imports =
        jdbcTemplate.query(
            "SELECT id,controlled_source_reference,created_at FROM resume_imports WHERE id=? AND user_id=?",
            (row, number) ->
                new ResumeImport(
                    row.getObject("id", UUID.class),
                    userId,
                    row.getString("controlled_source_reference"),
                    row.getTimestamp("created_at").toInstant(),
                    jdbcTemplate.query(
                        "SELECT id,candidate_type,payload::text,source_location,review_status FROM fact_candidates WHERE import_id=? ORDER BY created_at,id",
                        (candidateRow, candidateNumber) ->
                            FactCandidate.restore(
                                candidateRow.getObject("id", UUID.class),
                                candidateRow.getString("candidate_type"),
                                candidateRow.getString("payload"),
                                candidateRow.getString("source_location"),
                                CandidateReviewStatus.valueOf(
                                    candidateRow.getString("review_status"))),
                        importId)),
            importId,
            userId.value());
    return imports.stream().findFirst();
  }

  @Override
  @Transactional
  public CandidateDecision saveDecision(UserId userId, CandidateDecision decision) {
    try {
      jdbcTemplate.update(
          "INSERT INTO candidate_decisions(id,candidate_id,user_id,decision_type,edited_payload,command_id,decided_at) VALUES (?,?,?, ?,CAST(? AS JSONB),?,?)",
          UUID.randomUUID(),
          decision.candidateId(),
          userId.value(),
          decision.type().name(),
          decision.editedPayload(),
          decision.commandId(),
          Timestamp.from(decision.decidedAt()));
    } catch (DuplicateKeyException exception) {
      throw new IllegalStateException("该 commandId 已处理", exception);
    }
    CandidateReviewStatus status =
        switch (decision.type()) {
          case CONFIRM -> CandidateReviewStatus.CONFIRMED;
          case EDIT -> CandidateReviewStatus.EDITED;
          case REJECT -> CandidateReviewStatus.REJECTED;
        };
    int updated =
        jdbcTemplate.update(
            "UPDATE fact_candidates SET review_status=?,updated_at=CURRENT_TIMESTAMP,version=version+1 WHERE id=? AND review_status='WAITING_CONFIRMATION' AND import_id IN (SELECT id FROM resume_imports WHERE user_id=?)",
            status.name(),
            decision.candidateId(),
            userId.value());
    if (updated != 1) throw new IllegalStateException("候选项当前状态不允许作出决定");
    return decision;
  }
}
