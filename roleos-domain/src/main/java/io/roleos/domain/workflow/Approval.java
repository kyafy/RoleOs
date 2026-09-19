package io.roleos.domain.workflow;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A human-control checkpoint. Only a pending approval can be resolved. */
public record Approval(
    UUID id,
    UUID workflowId,
    UUID userId,
    ApprovalStatus status,
    ApprovalDecision decision,
    String note,
    Instant createdAt,
    Instant resolvedAt) {
  public Approval {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public boolean isPending() {
    return status == ApprovalStatus.PENDING || status == ApprovalStatus.DEFERRED;
  }
}
