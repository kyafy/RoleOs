package io.roleos.application.career.workflow;

import io.roleos.domain.workflow.*;
import io.roleos.workflow.WorkflowTransitionService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Application boundary: agents never receive a mutable workflow-state API. */
@Service
public final class WorkflowApplicationService {
  private static final String LOG_COMMAND_ID = "commandId";
  private static final String LOG_EVENT = "event";
  private static final String LOG_STATUS = "status";
  private static final String LOG_WORKFLOW_ID = "workflowId";
  private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowApplicationService.class);

  private final WorkflowRepository repo;
  private final WorkflowTransitionService transitions;
  private final Clock clock;

  public WorkflowApplicationService(
      WorkflowRepository repo, WorkflowTransitionService transitions, Clock clock) {
    this.repo = repo;
    this.transitions = transitions;
    this.clock = clock;
  }

  public WorkflowInstance create(
      UUID user, WorkflowType type, WorkflowStage stage, String reason, String commandId) {
    var prior = repo.findByCommandId(user, commandId);
    if (prior.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "workflow.create.idempotency_hit")
          .addKeyValue(LOG_COMMAND_ID, commandId)
          .log("工作流创建命令已幂等命中");
      return prior.get();
    }
    var now = clock.instant();
    var w =
        new WorkflowInstance(
            UUID.randomUUID(),
            user,
            type,
            stage,
            WorkflowStatus.WAITING_APPROVAL,
            reason,
            0,
            0,
            now,
            now);
    repo.create(
        w,
        new Approval(
            UUID.randomUUID(), w.id(), user, ApprovalStatus.PENDING, null, reason, now, null),
        commandId);
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "workflow.created")
        .addKeyValue(LOG_WORKFLOW_ID, w.id())
        .addKeyValue("workflowType", type)
        .addKeyValue("stage", stage)
        .addKeyValue(LOG_STATUS, w.status())
        .addKeyValue(LOG_COMMAND_ID, commandId)
        .log("工作流已创建并等待人工审批");
    return w;
  }

  public List<WorkflowInstance> list(UUID user) {
    return repo.findAll(user);
  }

  public List<Approval> approvals(UUID user) {
    return repo.findApprovals(user);
  }

  public WorkflowInstance decide(
      UUID user, UUID approvalId, ApprovalDecision decision, String commandId) {
    var prior = repo.findByCommandId(user, commandId);
    if (prior.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "workflow.approval.idempotency_hit")
          .addKeyValue("approvalId", approvalId)
          .addKeyValue(LOG_COMMAND_ID, commandId)
          .log("工作流审批命令已幂等命中");
      return prior.get();
    }
    Approval a =
        repo.findApproval(user, approvalId)
            .orElseThrow(() -> new IllegalArgumentException("审批不存在"));
    WorkflowInstance w =
        repo.find(user, a.workflowId()).orElseThrow(() -> new IllegalArgumentException("工作流不存在"));
    var result = transitions.decideApproval(w, a, user, decision, commandId);
    Approval resolved =
        new Approval(
            a.id(),
            a.workflowId(),
            a.userId(),
            a.status(),
            decision,
            a.note(),
            a.createdAt(),
            result.decidedAt());
    WorkflowInstance saved = repo.saveTransition(result.workflow(), resolved, commandId);
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "workflow.approval.decided")
        .addKeyValue(LOG_WORKFLOW_ID, saved.id())
        .addKeyValue("approvalId", approvalId)
        .addKeyValue("decision", decision)
        .addKeyValue(LOG_STATUS, saved.status())
        .addKeyValue(LOG_COMMAND_ID, commandId)
        .log("工作流审批决定已持久化");
    return saved;
  }

  public WorkflowInstance resume(UUID user, UUID id, String commandId) {
    var prior = repo.findByCommandId(user, commandId);
    if (prior.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "workflow.resume.idempotency_hit")
          .addKeyValue(LOG_WORKFLOW_ID, id)
          .addKeyValue(LOG_COMMAND_ID, commandId)
          .log("工作流恢复命令已幂等命中");
      return prior.get();
    }
    WorkflowInstance w =
        repo.find(user, id).orElseThrow(() -> new IllegalArgumentException("工作流不存在"));
    if (w.status() != WorkflowStatus.PAUSED_FOR_HUMAN
        && w.status() != WorkflowStatus.WAITING_USER_INPUT)
      throw new IllegalStateException("当前工作流不能显式恢复");
    WorkflowInstance resumed =
        repo.saveState(
            w.transitionTo(WorkflowStatus.RUNNING, null, clock.instant()), w.status(), commandId);
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "workflow.resumed")
        .addKeyValue(LOG_WORKFLOW_ID, resumed.id())
        .addKeyValue("fromStatus", w.status())
        .addKeyValue(LOG_STATUS, resumed.status())
        .addKeyValue(LOG_COMMAND_ID, commandId)
        .log("工作流已从人工等待状态恢复");
    return resumed;
  }

  public WorkflowInstance cancel(UUID user, UUID id, String commandId) {
    var prior = repo.findByCommandId(user, commandId);
    if (prior.isPresent()) {
      LOGGER
          .atInfo()
          .addKeyValue(LOG_EVENT, "workflow.cancel.idempotency_hit")
          .addKeyValue(LOG_WORKFLOW_ID, id)
          .addKeyValue(LOG_COMMAND_ID, commandId)
          .log("工作流取消命令已幂等命中");
      return prior.get();
    }
    WorkflowInstance w =
        repo.find(user, id).orElseThrow(() -> new IllegalArgumentException("工作流不存在"));
    WorkflowInstance cancelled =
        repo.saveState(
            w.transitionTo(WorkflowStatus.CANCELLED, "用户取消", clock.instant()),
            w.status(),
            commandId);
    LOGGER
        .atInfo()
        .addKeyValue(LOG_EVENT, "workflow.cancelled")
        .addKeyValue(LOG_WORKFLOW_ID, cancelled.id())
        .addKeyValue("fromStatus", w.status())
        .addKeyValue(LOG_STATUS, cancelled.status())
        .addKeyValue(LOG_COMMAND_ID, commandId)
        .log("工作流已取消");
    return cancelled;
  }
}
