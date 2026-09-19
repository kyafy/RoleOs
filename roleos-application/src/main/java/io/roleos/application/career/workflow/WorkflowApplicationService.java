package io.roleos.application.career.workflow;

import io.roleos.domain.workflow.*;
import io.roleos.workflow.WorkflowTransitionService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application boundary: agents never receive a mutable workflow-state API. */
@Service
public final class WorkflowApplicationService {
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
    return repo.saveTransition(result.workflow(), resolved, commandId);
  }

  public WorkflowInstance resume(UUID user, UUID id, String commandId) {
    var prior = repo.findByCommandId(user, commandId);
    if (prior.isPresent()) {
      return prior.get();
    }
    WorkflowInstance w =
        repo.find(user, id).orElseThrow(() -> new IllegalArgumentException("工作流不存在"));
    if (w.status() != WorkflowStatus.PAUSED_FOR_HUMAN
        && w.status() != WorkflowStatus.WAITING_USER_INPUT)
      throw new IllegalStateException("当前工作流不能显式恢复");
    return repo.saveState(
        w.transitionTo(WorkflowStatus.RUNNING, null, clock.instant()), w.status(), commandId);
  }

  public WorkflowInstance cancel(UUID user, UUID id, String commandId) {
    var prior = repo.findByCommandId(user, commandId);
    if (prior.isPresent()) {
      return prior.get();
    }
    WorkflowInstance w =
        repo.find(user, id).orElseThrow(() -> new IllegalArgumentException("工作流不存在"));
    return repo.saveState(
        w.transitionTo(WorkflowStatus.CANCELLED, "用户取消", clock.instant()), w.status(), commandId);
  }
}
