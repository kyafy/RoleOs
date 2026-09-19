package io.roleos.domain.career;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Provenance appended when a reviewed candidate becomes a confirmed career fact. */
public record CareerFactSource(
    UUID id,
    UserId userId,
    String assetType,
    UUID assetId,
    ProvenanceType provenanceType,
    ConfirmationStatus confirmationStatus,
    String sourceReference,
    Instant createdAt) {
  public CareerFactSource {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    if (assetType == null || assetType.isBlank())
      throw new IllegalArgumentException("assetType must not be blank");
    Objects.requireNonNull(assetId, "assetId");
    Objects.requireNonNull(provenanceType, "provenanceType");
    Objects.requireNonNull(confirmationStatus, "confirmationStatus");
    if (sourceReference == null || sourceReference.isBlank())
      throw new IllegalArgumentException("sourceReference must not be blank");
    Objects.requireNonNull(createdAt, "createdAt");
  }
}
