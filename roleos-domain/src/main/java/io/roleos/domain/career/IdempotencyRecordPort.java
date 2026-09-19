package io.roleos.domain.career;

import java.util.Optional;

/** 幂等记录端口；实现必须对 (userId, commandId) 建立唯一约束。 */
public interface IdempotencyRecordPort {
  Optional<IdempotencyRecord> findBy(UserId userId, CommandId commandId);

  IdempotencyRecord saveIfAbsent(IdempotencyRecord record);
}
