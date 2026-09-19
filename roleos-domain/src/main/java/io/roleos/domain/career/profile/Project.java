package io.roleos.domain.career.profile;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import java.util.Objects;
import java.util.UUID;

/** 可独立讨论的项目；性质和历史来源均为必填，避免混同生产经历。 */
public record Project(
    UUID id,
    UserId userId,
    String name,
    ProjectNature nature,
    String historicalSource,
    ProvenanceType provenanceType,
    ConfirmationStatus confirmationStatus,
    AuditFields auditFields) {
  public Project {
    Objects.requireNonNull(id, "项目标识不能为空");
    Objects.requireNonNull(userId, "用户不能为空");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("项目名称不能为空");
    Objects.requireNonNull(nature, "项目性质不能为空");
    if (historicalSource == null || historicalSource.isBlank())
      throw new IllegalArgumentException("项目历史来源不能为空");
    if (provenanceType == ProvenanceType.AGENT_INFERENCE)
      throw new IllegalArgumentException("推断不得直接创建项目");
    if (confirmationStatus != ConfirmationStatus.CONFIRMED)
      throw new IllegalArgumentException("项目必须经过确认");
    Objects.requireNonNull(auditFields, "审计字段不能为空");
  }
}
