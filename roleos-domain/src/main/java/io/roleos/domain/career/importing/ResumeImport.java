package io.roleos.domain.career.importing;

import io.roleos.domain.career.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlled import batch. The raw resume is represented only by its protected source reference.
 */
public record ResumeImport(
    UUID id,
    UserId userId,
    String controlledSourceReference,
    Instant createdAt,
    List<FactCandidate> candidates) {
  public ResumeImport {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    if (controlledSourceReference == null || controlledSourceReference.isBlank()) {
      throw new IllegalArgumentException("controlledSourceReference must not be blank");
    }
    Objects.requireNonNull(createdAt, "createdAt");
    candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
  }
}
