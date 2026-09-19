package io.roleos.domain.career.skill;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Explicit, user-maintained capability relationships; never a calculated score. */
public record CapabilityLinks(
    UUID capabilityId, List<UUID> skillIds, List<UUID> experienceIds, List<UUID> projectIds) {
  public CapabilityLinks {
    Objects.requireNonNull(capabilityId, "capabilityId");
    skillIds = List.copyOf(Objects.requireNonNull(skillIds, "skillIds"));
    experienceIds = List.copyOf(Objects.requireNonNull(experienceIds, "experienceIds"));
    projectIds = List.copyOf(Objects.requireNonNull(projectIds, "projectIds"));
  }
}
