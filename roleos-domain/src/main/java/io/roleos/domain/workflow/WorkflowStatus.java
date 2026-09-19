package io.roleos.domain.workflow;

/** State is owned by the workflow state machine, never by an agent response. */
public enum WorkflowStatus {
  RUNNING,
  WAITING_USER_INPUT,
  WAITING_APPROVAL,
  PAUSED_FOR_HUMAN,
  RETRYABLE_FAILED,
  FAILED,
  COMPLETED,
  CANCELLED;

  public boolean isTerminal() {
    return this == FAILED || this == COMPLETED || this == CANCELLED;
  }
}
