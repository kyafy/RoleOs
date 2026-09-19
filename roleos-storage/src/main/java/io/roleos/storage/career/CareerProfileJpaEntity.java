package io.roleos.storage.career;

import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** JPA 持久化模型；不向领域层泄露 JPA 注解或类型。 */
@Entity
@Table(name = "career_profile")
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
class CareerProfileJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "career_direction", nullable = false)
  private String careerDirection;

  @Column(name = "target_role_preference", nullable = false)
  private String targetRolePreference;

  @Enumerated(EnumType.STRING)
  @Column(name = "provenance_type", nullable = false)
  private ProvenanceType provenanceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "confirmation_status", nullable = false)
  private ConfirmationStatus confirmationStatus;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected CareerProfileJpaEntity() {}

  CareerProfileJpaEntity(
      UUID id,
      UUID userId,
      String displayName,
      String careerDirection,
      String targetRolePreference,
      ProvenanceType provenanceType,
      ConfirmationStatus confirmationStatus,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.userId = userId;
    this.displayName = displayName;
    this.careerDirection = careerDirection;
    this.targetRolePreference = targetRolePreference;
    this.provenanceType = provenanceType;
    this.confirmationStatus = confirmationStatus;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  UUID id() {
    return id;
  }

  UUID userId() {
    return userId;
  }

  String displayName() {
    return displayName;
  }

  String careerDirection() {
    return careerDirection;
  }

  String targetRolePreference() {
    return targetRolePreference;
  }

  ProvenanceType provenanceType() {
    return provenanceType;
  }

  ConfirmationStatus confirmationStatus() {
    return confirmationStatus;
  }

  Instant createdAt() {
    return createdAt;
  }

  Instant updatedAt() {
    return updatedAt;
  }

  long version() {
    return version;
  }
}
