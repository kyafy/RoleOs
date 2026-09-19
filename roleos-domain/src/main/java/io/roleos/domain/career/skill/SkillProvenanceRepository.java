package io.roleos.domain.career.skill;

import io.roleos.domain.career.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for sources and explicit capability associations. */
public interface SkillProvenanceRepository {
  List<SkillSource> findSources(UserId userId, UUID skillId);

  SkillSource saveSource(UserId userId, SkillSource source);

  List<Capability> findCapabilities(UserId userId);

  Optional<Capability> findCapability(UserId userId, UUID capabilityId);

  Capability saveCapability(Capability capability);

  CapabilityLinks links(UserId userId, UUID capabilityId);

  CapabilityLinks replaceLinks(UserId userId, CapabilityLinks links);
}
