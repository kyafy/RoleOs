package io.roleos.storage.career;

import io.roleos.domain.workflow.Approval;
import io.roleos.domain.workflow.ApprovalDecision;
import io.roleos.domain.workflow.ApprovalStatus;
import io.roleos.domain.workflow.WorkflowInstance;
import io.roleos.domain.workflow.WorkflowRepository;
import io.roleos.domain.workflow.WorkflowStage;
import io.roleos.domain.workflow.WorkflowStatus;
import io.roleos.domain.workflow.WorkflowType;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Atomic workflow, approval, event and idempotency persistence. */
@Repository
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class JdbcWorkflowRepositoryAdapter implements WorkflowRepository {
  private final JdbcTemplate jdbc;

  public JdbcWorkflowRepositoryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public WorkflowInstance create(WorkflowInstance w, Approval a, String commandId) {
    jdbc.update(
        "INSERT INTO workflow_instances(id,user_id,workflow_type,current_stage,status,waiting_reason,retry_count,version,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
        w.id(),
        w.userId(),
        w.type().name(),
        w.stage().name(),
        w.status().name(),
        w.waitingReason(),
        w.retryCount(),
        w.version(),
        Timestamp.from(w.createdAt()),
        Timestamp.from(w.updatedAt()));
    jdbc.update(
        "INSERT INTO approvals(id,workflow_id,user_id,status,decision,note,created_at) VALUES (?,?,?,?,?,?,?)",
        a.id(),
        a.workflowId(),
        a.userId(),
        a.status().name(),
        null,
        a.note(),
        Timestamp.from(a.createdAt()));
    jdbc.update(
        "INSERT INTO workflow_idempotency_records(command_id,workflow_id,result_status) VALUES (?,?,?)",
        commandId,
        w.id(),
        w.status().name());
    return w;
  }

  @Override
  public List<WorkflowInstance> findAll(UUID u) {
    return jdbc.query(
        "SELECT * FROM workflow_instances WHERE user_id=? ORDER BY created_at,id",
        (r, n) -> map(r),
        u);
  }

  @Override
  public Optional<WorkflowInstance> find(UUID u, UUID id) {
    return jdbc
        .query("SELECT * FROM workflow_instances WHERE id=? AND user_id=?", (r, n) -> map(r), id, u)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<WorkflowInstance> findByCommandId(UUID u, String commandId) {
    return jdbc
        .query(
            "SELECT w.* FROM workflow_idempotency_records i JOIN workflow_instances w ON w.id=i.workflow_id WHERE i.command_id=? AND w.user_id=?",
            (r, n) -> map(r),
            commandId,
            u)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Approval> findApproval(UUID u, UUID id) {
    return jdbc
        .query(
            "SELECT a.* FROM approvals a JOIN workflow_instances w ON w.id=a.workflow_id WHERE a.id=? AND a.user_id=? AND w.user_id=?",
            (r, n) -> mapApproval(r),
            id,
            u,
            u)
        .stream()
        .findFirst();
  }

  @Override
  public List<Approval> findApprovals(UUID u) {
    return jdbc.query(
        "SELECT * FROM approvals WHERE user_id=? ORDER BY created_at,id",
        (r, n) -> mapApproval(r),
        u);
  }

  @Override
  @Transactional
  public WorkflowInstance saveTransition(WorkflowInstance w, Approval a, String cmd) {
    var existing = findByCommandId(w.userId(), cmd);
    if (existing.isPresent()) {
      return existing.get();
    }
    WorkflowStatus previous = WorkflowStatus.WAITING_APPROVAL;
    if (w.status() != WorkflowStatus.WAITING_APPROVAL) {
      int count =
          jdbc.update(
              "UPDATE workflow_instances SET status=?,waiting_reason=?,version=?,updated_at=? WHERE id=? AND user_id=? AND version=?",
              w.status().name(),
              w.waitingReason(),
              w.version(),
              Timestamp.from(w.updatedAt()),
              w.id(),
              w.userId(),
              w.version() - 1);
      if (count != 1) {
        throw new IllegalStateException("工作流状态冲突");
      }
    }
    ApprovalStatus as =
        switch (a.decision()) {
          case APPROVE -> ApprovalStatus.APPROVED;
          case REJECT -> ApprovalStatus.REJECTED;
          case REQUEST_CHANGES -> ApprovalStatus.CHANGES_REQUESTED;
          case DEFER -> ApprovalStatus.DEFERRED;
        };
    jdbc.update(
        "UPDATE approvals SET status=?,decision=?,resolved_at=?,version=version+1 WHERE id=? AND user_id=?",
        as.name(),
        a.decision().name(),
        Timestamp.from(a.resolvedAt()),
        a.id(),
        a.userId());
    jdbc.update(
        "INSERT INTO workflow_events(id,workflow_id,command_id,from_status,to_status,occurred_at) VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        w.id(),
        cmd,
        previous.name(),
        w.status().name(),
        Timestamp.from(w.updatedAt()));
    jdbc.update(
        "INSERT INTO workflow_idempotency_records(command_id,workflow_id,result_status) VALUES (?,?,?) ON CONFLICT DO NOTHING",
        cmd,
        w.id(),
        w.status().name());
    return w;
  }

  @Override
  @Transactional
  public WorkflowInstance saveState(WorkflowInstance w, WorkflowStatus previous, String cmd) {
    var existing = findByCommandId(w.userId(), cmd);
    if (existing.isPresent()) {
      return existing.get();
    }
    int count =
        jdbc.update(
            "UPDATE workflow_instances SET status=?,waiting_reason=?,version=?,updated_at=? WHERE id=? AND user_id=? AND version=?",
            w.status().name(),
            w.waitingReason(),
            w.version(),
            Timestamp.from(w.updatedAt()),
            w.id(),
            w.userId(),
            w.version() - 1);
    if (count != 1) {
      throw new IllegalStateException("工作流状态冲突");
    }
    jdbc.update(
        "INSERT INTO workflow_events(id,workflow_id,command_id,from_status,to_status,occurred_at) VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        w.id(),
        cmd,
        previous.name(),
        w.status().name(),
        Timestamp.from(w.updatedAt()));
    jdbc.update(
        "INSERT INTO workflow_idempotency_records(command_id,workflow_id,result_status) VALUES (?,?,?)",
        cmd,
        w.id(),
        w.status().name());
    return w;
  }

  private Approval mapApproval(java.sql.ResultSet r) throws java.sql.SQLException {
    return new Approval(
        r.getObject("id", UUID.class),
        r.getObject("workflow_id", UUID.class),
        r.getObject("user_id", UUID.class),
        ApprovalStatus.valueOf(r.getString("status")),
        r.getString("decision") == null ? null : ApprovalDecision.valueOf(r.getString("decision")),
        r.getString("note"),
        r.getTimestamp("created_at").toInstant(),
        r.getTimestamp("resolved_at") == null ? null : r.getTimestamp("resolved_at").toInstant());
  }

  private WorkflowInstance map(java.sql.ResultSet r) throws java.sql.SQLException {
    return new WorkflowInstance(
        r.getObject("id", UUID.class),
        r.getObject("user_id", UUID.class),
        WorkflowType.valueOf(r.getString("workflow_type")),
        WorkflowStage.valueOf(r.getString("current_stage")),
        WorkflowStatus.valueOf(r.getString("status")),
        r.getString("waiting_reason"),
        r.getInt("retry_count"),
        r.getLong("version"),
        r.getTimestamp("created_at").toInstant(),
        r.getTimestamp("updated_at").toInstant());
  }
}
