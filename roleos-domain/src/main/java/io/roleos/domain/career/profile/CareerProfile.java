package io.roleos.domain.career.profile;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import java.util.Objects;
import java.util.UUID;

/** 用户确认的职业档案。 */
public record CareerProfile(
    UUID id,
    UserId userId,
    String displayName,
    String careerDirection,
    String targetRolePreference,
    ProvenanceType provenanceType,
    ConfirmationStatus confirmationStatus,
    AuditFields auditFields) {
  public CareerProfile {
    Objects.requireNonNull(id, "档案标识不能为空");
    Objects.requireNonNull(userId, "用户不能为空");
    requireText(displayName, "显示名称不能为空");
    requireText(careerDirection, "职业方向不能为空");
    requireText(targetRolePreference, "目标岗位偏好不能为空");
    if (provenanceType != ProvenanceType.USER_INPUT
        && provenanceType != ProvenanceType.USER_CONFIRMED) {
      throw new IllegalArgumentException("档案只能由用户输入或用户确认创建");
    }
    if (confirmationStatus != ConfirmationStatus.CONFIRMED) {
      throw new IllegalArgumentException("档案必须是已确认职业事实");
    }
    Objects.requireNonNull(auditFields, "审计字段不能为空");
  }

  private static void requireText(String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
  }
}
