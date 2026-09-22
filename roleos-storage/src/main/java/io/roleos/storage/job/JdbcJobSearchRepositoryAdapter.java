package io.roleos.storage.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobSearch;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.SearchState;
import io.roleos.job.domain.JobTypes.SearchStep;
import io.roleos.job.port.JobRepositories.JobSearchRepository;
import io.roleos.storage.career.CareerUserJdbcSupport;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL 可恢复岗位搜索状态适配器。 */
@Repository
@SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
public class JdbcJobSearchRepositoryAdapter implements JobSearchRepository {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final CareerUserJdbcSupport users;
  private final ObjectMapper objectMapper;

  @SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
  public JdbcJobSearchRepositoryAdapter(
      JdbcTemplate jdbc, CareerUserJdbcSupport users, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.users = users;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<JobSearch> find(UserId userId, UUID searchId) {
    return jdbc
        .query(
            "SELECT * FROM job_search WHERE user_id=? AND id=?",
            this::map,
            userId.value(),
            searchId)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<JobSearch> findByStartCommandId(UserId userId, String commandId) {
    return findByCommand(userId, "start_command_id", commandId);
  }

  @Override
  public Optional<JobSearch> findByResumeCommandId(UserId userId, String commandId) {
    return findByCommand(userId, "resume_command_id", commandId);
  }

  @Override
  public JobSearch save(JobSearch search) {
    users.ensure(search.userId());
    jdbc.update(
        """
        INSERT INTO job_search(
          id,user_id,criteria_json,start_command_id,resume_command_id,requested_provider,
          active_provider,state,step,resume_url,cursor_value,wait_reason,failure_code,
          attempt_count,discovered_count,normalized_count,rejected_count,ranked_count,
          started_at,updated_at,completed_at)
        VALUES (?,?,CAST(? AS jsonb),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        ON CONFLICT (id) DO UPDATE SET
          resume_command_id=EXCLUDED.resume_command_id,active_provider=EXCLUDED.active_provider,
          state=EXCLUDED.state,step=EXCLUDED.step,
          resume_url=EXCLUDED.resume_url,cursor_value=EXCLUDED.cursor_value,
          wait_reason=EXCLUDED.wait_reason,failure_code=EXCLUDED.failure_code,
          attempt_count=EXCLUDED.attempt_count,discovered_count=EXCLUDED.discovered_count,
          normalized_count=EXCLUDED.normalized_count,rejected_count=EXCLUDED.rejected_count,
          ranked_count=EXCLUDED.ranked_count,updated_at=EXCLUDED.updated_at,
          completed_at=EXCLUDED.completed_at
        WHERE job_search.user_id=EXCLUDED.user_id
        """,
        search.id(),
        search.userId().value(),
        json(search.criteria()),
        search.startCommandId(),
        search.resumeCommandId(),
        search.requestedProvider().name(),
        name(search.activeProvider()),
        search.state().name(),
        search.step().name(),
        search.resumeUrl() == null ? null : search.resumeUrl().toString(),
        search.cursor(),
        search.waitReason(),
        search.failureCode(),
        search.attemptCount(),
        search.counts().discovered(),
        search.counts().normalized(),
        search.counts().rejected(),
        search.counts().ranked(),
        Timestamp.from(search.startedAt()),
        Timestamp.from(search.updatedAt()),
        timestamp(search.completedAt()));
    return find(search.userId(), search.id()).orElseThrow();
  }

  @SuppressWarnings(
      "PMD.UnusedFormalParameter") // JdbcTemplate RowMapper contract supplies row number.
  private JobSearch map(ResultSet row, int number) throws SQLException {
    String activeProvider = row.getString("active_provider");
    String resumeUrl = row.getString("resume_url");
    return new JobSearch(
        row.getObject("id", UUID.class),
        new UserId(row.getObject("user_id", UUID.class)),
        readMap(row.getString("criteria_json")),
        row.getString("start_command_id"),
        row.getString("resume_command_id"),
        BrowserProviderType.valueOf(row.getString("requested_provider")),
        activeProvider == null ? null : BrowserProviderType.valueOf(activeProvider),
        SearchState.valueOf(row.getString("state")),
        SearchStep.valueOf(row.getString("step")),
        resumeUrl == null ? null : URI.create(resumeUrl),
        row.getString("cursor_value"),
        row.getString("wait_reason"),
        row.getString("failure_code"),
        row.getInt("attempt_count"),
        new JobSearch.Counts(
            row.getInt("discovered_count"),
            row.getInt("normalized_count"),
            row.getInt("rejected_count"),
            row.getInt("ranked_count")),
        row.getTimestamp("started_at").toInstant(),
        row.getTimestamp("updated_at").toInstant(),
        instant(row, "completed_at"));
  }

  private Optional<JobSearch> findByCommand(UserId userId, String column, String commandId) {
    return jdbc
        .query(
            "SELECT * FROM job_search WHERE user_id=? AND " + column + "=?",
            this::map,
            userId.value(),
            commandId)
        .stream()
        .findFirst();
  }

  private String json(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("搜索条件无法序列化", exception);
    }
  }

  private Map<String, Object> readMap(String value) {
    try {
      return objectMapper.readValue(value, MAP_TYPE);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("搜索条件无法读取", exception);
    }
  }

  private static String name(Enum<?> value) {
    return value == null ? null : value.name();
  }

  private static Timestamp timestamp(java.time.Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private static java.time.Instant instant(ResultSet row, String column) throws SQLException {
    Timestamp value = row.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
