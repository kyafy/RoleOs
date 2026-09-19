package io.roleos.domain.career;

import java.time.Instant;
import java.util.Objects;

/** 持久化职业事实必须保留的审计字段。 */
public record AuditFields(Instant createdAt, Instant updatedAt, long version) {
  public AuditFields {
    Objects.requireNonNull(createdAt, "创建时间不能为空");
    Objects.requireNonNull(updatedAt, "更新时间不能为空");
    if (updatedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("更新时间不能早于创建时间");
    }
    if (version < 0) {
      throw new IllegalArgumentException("乐观锁版本不能为负数");
    }
  }
}
