package io.roleos.domain.career.importing;

import java.util.Objects;
import java.util.UUID;

/**
 * A proposed fact extracted from a controlled resume fixture. It is deliberately not a CareerFact:
 * its payload can only be promoted after an explicit decision.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class FactCandidate {
  private final UUID id;
  private final String candidateType;
  private final String payload;
  private final String sourceLocation;
  private CandidateReviewStatus status;

  public FactCandidate(UUID id, String candidateType, String payload, String sourceLocation) {
    this(id, candidateType, payload, sourceLocation, CandidateReviewStatus.WAITING_CONFIRMATION);
  }

  private FactCandidate(
      UUID id,
      String candidateType,
      String payload,
      String sourceLocation,
      CandidateReviewStatus status) {
    this.id = Objects.requireNonNull(id, "id");
    this.candidateType = requireText(candidateType, "candidateType");
    this.payload = requireText(payload, "payload");
    this.sourceLocation = requireText(sourceLocation, "sourceLocation");
    this.status = Objects.requireNonNull(status, "status");
  }

  /** Rehydrates a candidate without turning it into a career fact. */
  public static FactCandidate restore(
      UUID id,
      String candidateType,
      String payload,
      String sourceLocation,
      CandidateReviewStatus status) {
    return new FactCandidate(id, candidateType, payload, sourceLocation, status);
  }

  public UUID id() {
    return id;
  }

  public String candidateType() {
    return candidateType;
  }

  public String payload() {
    return payload;
  }

  public String sourceLocation() {
    return sourceLocation;
  }

  public CandidateReviewStatus status() {
    return status;
  }

  public void decide(CandidateDecision decision) {
    Objects.requireNonNull(decision, "decision");
    if (status != CandidateReviewStatus.WAITING_CONFIRMATION) {
      throw new IllegalStateException("a candidate may only be decided once");
    }
    status =
        switch (decision.type()) {
          case CONFIRM -> CandidateReviewStatus.CONFIRMED;
          case EDIT -> CandidateReviewStatus.EDITED;
          case REJECT -> CandidateReviewStatus.REJECTED;
        };
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException(name + " must not be blank");
    return value;
  }
}
