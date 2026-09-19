package io.roleos.domain.career;

import java.util.Objects;
import java.util.UUID;

/** 写命令的幂等键；同一用户的同一键只能产生一次结果。 */
public record CommandId(UUID value) {
  public CommandId {
    Objects.requireNonNull(value, "commandId 不能为空");
  }
}
