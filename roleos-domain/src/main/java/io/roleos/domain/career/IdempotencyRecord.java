package io.roleos.domain.career;

import java.time.Instant;
import java.util.Objects;

/** 已完成写命令的确定性结果记录。 */
public record IdempotencyRecord(
    UserId userId, CommandId commandId, String resultReference, Instant createdAt) {
  public IdempotencyRecord {
    Objects.requireNonNull(userId, "用户不能为空");
    Objects.requireNonNull(commandId, "commandId 不能为空");
    if (resultReference == null || resultReference.isBlank()) {
      throw new IllegalArgumentException("结果引用不能为空");
    }
    Objects.requireNonNull(createdAt, "创建时间不能为空");
  }
}
