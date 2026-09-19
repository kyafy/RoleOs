package io.roleos.storage.career;

import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.IdempotencyRecord;
import io.roleos.domain.career.IdempotencyRecordPort;
import io.roleos.domain.career.UserId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL 幂等记录适配器。 */
@Repository
public class JdbcIdempotencyRecordAdapter implements IdempotencyRecordPort {
  private final JdbcTemplate jdbcTemplate;
  private final CareerUserJdbcSupport users;

  public JdbcIdempotencyRecordAdapter(JdbcTemplate jdbcTemplate, CareerUserJdbcSupport users) {
    this.jdbcTemplate = jdbcTemplate;
    this.users = users;
  }

  @Override
  public Optional<IdempotencyRecord> findBy(UserId userId, CommandId commandId) {
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(
              "SELECT user_id, command_id, result_reference, created_at FROM idempotency_record WHERE user_id=? AND command_id=?",
              (row, number) ->
                  new IdempotencyRecord(
                      new UserId(row.getObject("user_id", UUID.class)),
                      new CommandId(row.getObject("command_id", UUID.class)),
                      row.getString("result_reference"),
                      row.getTimestamp("created_at").toInstant()),
              userId.value(),
              commandId.value()));
    } catch (EmptyResultDataAccessException ignored) {
      return Optional.empty();
    }
  }

  @Override
  public IdempotencyRecord saveIfAbsent(IdempotencyRecord record) {
    users.ensure(record.userId());
    jdbcTemplate.update(
        "INSERT INTO idempotency_record(id,user_id,command_id,result_reference,created_at) VALUES (?,?,?,?,?) ON CONFLICT (user_id,command_id) DO NOTHING",
        UUID.randomUUID(),
        record.userId().value(),
        record.commandId().value(),
        record.resultReference(),
        java.sql.Timestamp.from(record.createdAt()));
    return findBy(record.userId(), record.commandId()).orElseThrow();
  }
}
