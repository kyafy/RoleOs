package io.roleos.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.roleos.domain.workflow.Approval;
import io.roleos.domain.workflow.ApprovalDecision;
import io.roleos.domain.workflow.ApprovalStatus;
import io.roleos.domain.workflow.WorkflowInstance;
import io.roleos.domain.workflow.WorkflowStage;
import io.roleos.domain.workflow.WorkflowStatus;
import io.roleos.domain.workflow.WorkflowType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkflowTransitionServiceTest {
  private static final Instant NOW = Instant.parse("2026-09-17T00:00:00Z");
  private final WorkflowTransitionService service =
      new WorkflowTransitionService(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void deferredApprovalKeepsWorkflowWaiting() {
    WorkflowInstance workflow = workflow();
    var result =
        service.decideApproval(
            workflow, approval(workflow), workflow.userId(), ApprovalDecision.DEFER, "cmd-1");
    assertEquals(WorkflowStatus.WAITING_APPROVAL, result.workflow().status());
    assertEquals(0, result.workflow().version());
  }

  @Test
  void duplicateCommandDoesNotAdvanceTwice() {
    WorkflowInstance workflow = workflow();
    var first =
        service.decideApproval(
            workflow, approval(workflow), workflow.userId(), ApprovalDecision.APPROVE, "cmd-2");
    var duplicate =
        service.decideApproval(
            workflow, approval(workflow), workflow.userId(), ApprovalDecision.APPROVE, "cmd-2");
    assertEquals(first, duplicate);
    assertEquals(1, duplicate.workflow().version());
  }

  @Test
  void incompatibleWorkflowStateIsRejected() {
    WorkflowInstance completed =
        workflow()
            .transitionTo(WorkflowStatus.RUNNING, null, NOW)
            .transitionTo(WorkflowStatus.COMPLETED, null, NOW);
    assertThrows(
        IllegalStateException.class,
        () ->
            service.decideApproval(
                completed,
                approval(completed),
                completed.userId(),
                ApprovalDecision.APPROVE,
                "cmd-3"));
  }

  @Test
  void nonOwnerCannotUseTheTransitionEntryPointAsAnAgent() {
    WorkflowInstance workflow = workflow();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.decideApproval(
                workflow,
                approval(workflow),
                UUID.randomUUID(),
                ApprovalDecision.APPROVE,
                "agent-command"));
  }

  @Test
  void uncertainExternalEffectPausesForHumanInsteadOfRetrying() {
    WorkflowInstance running = workflow().transitionTo(WorkflowStatus.RUNNING, null, NOW);
    WorkflowInstance paused = service.pauseForUncertainExternalEffect(running, "提交是否已生效未知");
    assertEquals(WorkflowStatus.PAUSED_FOR_HUMAN, paused.status());
    assertEquals("提交是否已生效未知", paused.waitingReason());
  }

  private static WorkflowInstance workflow() {
    return new WorkflowInstance(
        UUID.randomUUID(),
        UUID.randomUUID(),
        WorkflowType.TARGETED_APPLY,
        WorkflowStage.APPLICATION_APPROVAL,
        WorkflowStatus.WAITING_APPROVAL,
        "等待用户确认",
        0,
        0,
        NOW,
        NOW);
  }

  private static Approval approval(WorkflowInstance workflow) {
    return new Approval(
        UUID.randomUUID(),
        workflow.id(),
        workflow.userId(),
        ApprovalStatus.PENDING,
        null,
        null,
        NOW,
        null);
  }
}
