package io.roleos.domain.career.skill;

import io.roleos.domain.career.UserId;
import java.util.Objects;
import java.util.UUID;

/** A named capability; no automatic score is stored or inferred in Career Foundation. */
public record Capability(UUID id, UserId userId, String name) {
  public Capability {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("name must not be blank");
  }
}
