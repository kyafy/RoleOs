package io.roleos.domain.career.skill;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** 原子职业技能；验证层级在 001 阶段刻意保持为空。 */
public record Skill(
    UUID id,
    UserId userId,
    String displayName,
    String normalizedName,
    SelfAssessmentLevel selfAssessmentLevel,
    SelfAssessmentLevel verifiedLevel,
    ProvenanceType provenanceType,
    ConfirmationStatus confirmationStatus,
    AuditFields auditFields) {
  public Skill {
    Objects.requireNonNull(id, "技能标识不能为空");
    Objects.requireNonNull(userId, "用户不能为空");
    if (displayName == null || displayName.isBlank())
      throw new IllegalArgumentException("技能名称不能为空");
    String expected = normalize(displayName);
    if (!expected.equals(normalizedName)) throw new IllegalArgumentException("技能标准名称必须由展示名称规范化得到");
    Objects.requireNonNull(selfAssessmentLevel, "自评层级不能为空");
    if (provenanceType != ProvenanceType.USER_INPUT
        && provenanceType != ProvenanceType.USER_CONFIRMED) {
      throw new IllegalArgumentException("技能必须由用户输入或用户确认创建");
    }
    if (confirmationStatus != ConfirmationStatus.CONFIRMED)
      throw new IllegalArgumentException("技能必须经过确认");
    Objects.requireNonNull(auditFields, "审计字段不能为空");
  }

  public static String normalize(String name) {
    if (name == null) throw new IllegalArgumentException("技能名称不能为空");
    String result = name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    if (result.isBlank()) throw new IllegalArgumentException("技能名称不能为空");
    return result;
  }
}
