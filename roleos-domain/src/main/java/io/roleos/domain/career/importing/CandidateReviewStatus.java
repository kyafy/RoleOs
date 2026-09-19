package io.roleos.domain.career.importing;

/** Lifecycle of an extracted candidate while it remains separate from career facts. */
public enum CandidateReviewStatus {
  WAITING_CONFIRMATION,
  CONFIRMED,
  EDITED,
  REJECTED
}
