package io.roleos.domain.workflow;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Append-only audit entry for a state-machine decision. */
public record WorkflowEvent(
    UUID id,
    UUID workflowId,
    String commandId,
    WorkflowStatus fromStatus,
    WorkflowStatus toStatus,
    Instant occurredAt) {
  public WorkflowEvent {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(commandId, "commandId must not be blank");
    Objects.requireNonNull(fromStatus, "fromStatus must not be null");
    Objects.requireNonNull(toStatus, "toStatus must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }
}
