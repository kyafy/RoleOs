package io.roleos.storage.career;

import io.roleos.domain.career.UserId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 在首次写入职业资产前建立最小用户归属记录。 */
@Component
final class CareerUserJdbcSupport {
  private final JdbcTemplate jdbcTemplate;

  CareerUserJdbcSupport(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  void ensure(UserId userId) {
    jdbcTemplate.update(
        "INSERT INTO career_user(id) VALUES (?) ON CONFLICT (id) DO NOTHING", userId.value());
  }
}
