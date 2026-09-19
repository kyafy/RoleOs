package io.roleos.workflow;

import io.roleos.domain.workflow.Approval;
import io.roleos.domain.workflow.ApprovalDecision;
import io.roleos.domain.workflow.WorkflowInstance;
import io.roleos.domain.workflow.WorkflowStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The sole in-process state-transition entry point. Persistence adapters must apply its outcome
 * atomically with the workflow, approval, event and command-id record.
 */
@Service
public final class WorkflowTransitionService {
  private final Clock clock;
  private final Map<String, WorkflowTransitionResult> completedCommands = new HashMap<>();

  public WorkflowTransitionService(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  public synchronized WorkflowTransitionResult decideApproval(
      WorkflowInstance workflow,
      Approval approval,
      UUID actorUserId,
      ApprovalDecision decision,
      String commandId) {
    requireCommandId(commandId);
    Objects.requireNonNull(workflow, "workflow must not be null");
    Objects.requireNonNull(approval, "approval must not be null");
    Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    if (!workflow.userId().equals(actorUserId) || !approval.userId().equals(actorUserId)) {
      throw new IllegalArgumentException("approval does not belong to the acting user");
    }
    if (!approval.workflowId().equals(workflow.id())) {
      throw new IllegalArgumentException("approval does not belong to workflow");
    }
    WorkflowTransitionResult prior = completedCommands.get(commandId);
    if (prior != null) {
      return prior;
    }
    if (workflow.status() != WorkflowStatus.WAITING_APPROVAL || !approval.isPending()) {
      throw new IllegalStateException("approval is no longer actionable");
    }
    Instant now = clock.instant();
    WorkflowStatus next =
        decision == ApprovalDecision.DEFER
            ? WorkflowStatus.WAITING_APPROVAL
            : WorkflowStatus.RUNNING;
    WorkflowInstance transitioned =
        decision == ApprovalDecision.DEFER ? workflow : workflow.transitionTo(next, null, now);
    WorkflowTransitionResult result = new WorkflowTransitionResult(transitioned, decision, now);
    completedCommands.put(commandId, result);
    return result;
  }

  public WorkflowInstance pauseForUncertainExternalEffect(
      WorkflowInstance workflow, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be blank");
    }
    return workflow.transitionTo(WorkflowStatus.PAUSED_FOR_HUMAN, reason, clock.instant());
  }

  private static void requireCommandId(String commandId) {
    if (commandId == null || commandId.isBlank()) {
      throw new IllegalArgumentException("commandId must not be blank");
    }
  }

  public record WorkflowTransitionResult(
      WorkflowInstance workflow, ApprovalDecision decision, Instant decidedAt) {}
}
