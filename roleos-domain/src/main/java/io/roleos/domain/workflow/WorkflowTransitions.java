package io.roleos.domain.workflow;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Deterministic transition rules for the persisted workflow. */
public final class WorkflowTransitions {
  private static final Map<WorkflowStatus, Set<WorkflowStatus>> ALLOWED =
      Map.of(
          WorkflowStatus.RUNNING,
          EnumSet.of(
              WorkflowStatus.WAITING_USER_INPUT,
              WorkflowStatus.WAITING_APPROVAL,
              WorkflowStatus.PAUSED_FOR_HUMAN,
              WorkflowStatus.RETRYABLE_FAILED,
              WorkflowStatus.COMPLETED,
              WorkflowStatus.CANCELLED),
          WorkflowStatus.WAITING_USER_INPUT,
          EnumSet.of(WorkflowStatus.RUNNING, WorkflowStatus.CANCELLED),
          WorkflowStatus.WAITING_APPROVAL,
          EnumSet.of(WorkflowStatus.RUNNING, WorkflowStatus.CANCELLED),
          WorkflowStatus.PAUSED_FOR_HUMAN,
          EnumSet.of(WorkflowStatus.RUNNING, WorkflowStatus.CANCELLED),
          WorkflowStatus.RETRYABLE_FAILED,
          EnumSet.of(WorkflowStatus.RUNNING, WorkflowStatus.FAILED, WorkflowStatus.CANCELLED));

  private WorkflowTransitions() {}

  public static void requireAllowed(WorkflowStatus current, WorkflowStatus next) {
    if (!ALLOWED.getOrDefault(current, EnumSet.noneOf(WorkflowStatus.class)).contains(next)) {
      throw new IllegalStateException("Illegal workflow transition: " + current + " -> " + next);
    }
  }
}
