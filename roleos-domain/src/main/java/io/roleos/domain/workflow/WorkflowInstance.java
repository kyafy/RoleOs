package io.roleos.domain.workflow;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable durable workflow aggregate view. */
public record WorkflowInstance(
    UUID id,
    UUID userId,
    WorkflowType type,
    WorkflowStage stage,
    WorkflowStatus status,
    String waitingReason,
    int retryCount,
    long version,
    Instant createdAt,
    Instant updatedAt) {
  public WorkflowInstance {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(stage, "stage must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (retryCount < 0 || version < 0) {
      throw new IllegalArgumentException("retryCount and version must not be negative");
    }
  }

  public WorkflowInstance transitionTo(WorkflowStatus next, String reason, Instant at) {
    WorkflowTransitions.requireAllowed(status, next);
    return new WorkflowInstance(
        id, userId, type, stage, next, reason, retryCount, version + 1, createdAt, at);
  }
}
