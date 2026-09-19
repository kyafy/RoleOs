package io.roleos.domain.career.importing;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable audit record for an explicit candidate decision. */
public record CandidateDecision(
    UUID candidateId,
    CandidateDecisionType type,
    String editedPayload,
    UUID commandId,
    Instant decidedAt) {
  public CandidateDecision {
    Objects.requireNonNull(candidateId, "candidateId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(commandId, "commandId");
    Objects.requireNonNull(decidedAt, "decidedAt");
    if (type == CandidateDecisionType.EDIT && (editedPayload == null || editedPayload.isBlank())) {
      throw new IllegalArgumentException("EDIT requires a non-empty edited payload");
    }
    if (type != CandidateDecisionType.EDIT && editedPayload != null) {
      throw new IllegalArgumentException("only EDIT may include an edited payload");
    }
  }
}
