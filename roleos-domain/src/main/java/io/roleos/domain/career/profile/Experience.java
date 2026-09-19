package io.roleos.domain.career.profile;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import java.util.Objects;
import java.util.UUID;

/** 用户真实经历，可保留不完整状态，绝不补造缺失事实。 */
public record Experience(
    UUID id,
    UserId userId,
    ExperienceType type,
    String title,
    String organization,
    boolean incomplete,
    ProvenanceType provenanceType,
    ConfirmationStatus confirmationStatus,
    AuditFields auditFields) {
  public Experience {
    Objects.requireNonNull(id, "经历标识不能为空");
    Objects.requireNonNull(userId, "用户不能为空");
    Objects.requireNonNull(type, "经历类型不能为空");
    if (title == null || title.isBlank()) throw new IllegalArgumentException("经历标题不能为空");
    if (organization == null) throw new IllegalArgumentException("经历组织信息不能为空");
    if (provenanceType == ProvenanceType.AGENT_INFERENCE)
      throw new IllegalArgumentException("推断不得直接创建经历");
    if (confirmationStatus != ConfirmationStatus.CONFIRMED)
      throw new IllegalArgumentException("经历必须经过确认");
    Objects.requireNonNull(auditFields, "审计字段不能为空");
  }
}
