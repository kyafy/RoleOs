package io.roleos.domain.career;

import java.util.Objects;
import java.util.UUID;

/** 职业数据归属用户的不可变标识。 */
public record UserId(UUID value) {
  public UserId {
    Objects.requireNonNull(value, "用户标识不能为空");
  }

  public static UserId random() {
    return new UserId(UUID.randomUUID());
  }
}
