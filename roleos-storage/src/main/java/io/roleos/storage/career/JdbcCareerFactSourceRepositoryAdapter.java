package io.roleos.storage.career;

import io.roleos.domain.career.CareerFactSource;
import io.roleos.domain.career.CareerFactSourceRepository;
import java.sql.Timestamp;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Appends provenance without exposing resume payloads in application logs. */
@Repository
public class JdbcCareerFactSourceRepositoryAdapter implements CareerFactSourceRepository {
  private final JdbcTemplate jdbcTemplate;
  private final CareerUserJdbcSupport users;

  public JdbcCareerFactSourceRepositoryAdapter(
      JdbcTemplate jdbcTemplate, CareerUserJdbcSupport users) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    this.users = Objects.requireNonNull(users);
  }

  @Override
  public CareerFactSource append(CareerFactSource source) {
    users.ensure(source.userId());
    jdbcTemplate.update(
        "INSERT INTO career_fact_source(id,user_id,asset_type,asset_id,provenance_type,confirmation_status,source_reference,created_at) VALUES (?,?,?,?,?,?,?,?)",
        source.id(),
        source.userId().value(),
        source.assetType(),
        source.assetId(),
        source.provenanceType().name(),
        source.confirmationStatus().name(),
        source.sourceReference(),
        Timestamp.from(source.createdAt()));
    return source;
  }
}
