package io.roleos.domain.career.skill;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Traceable source supporting a skill. A source does not by itself imply verification. */
public record SkillSource(
    UUID id,
    UUID skillId,
    SkillSourceType type,
    String supportingAssetReference,
    Instant recordedAt) {
  public SkillSource {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(skillId, "skillId");
    Objects.requireNonNull(type, "type");
    if (supportingAssetReference == null || supportingAssetReference.isBlank()) {
      throw new IllegalArgumentException("supportingAssetReference must not be blank");
    }
    Objects.requireNonNull(recordedAt, "recordedAt");
  }
}
