package io.roleos.storage.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobSearch.SourceSnapshot;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.ParseStatus;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.port.JobRepositories.JobRepository;
import io.roleos.job.port.JobRepositories.SourceSnapshotRepository;
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
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL Job 与 Raw Source Snapshot 适配器。 */
@Repository
@SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
public class JdbcJobRepositoryAdapter implements JobRepository, SourceSnapshotRepository {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final CareerUserJdbcSupport users;
  private final ObjectMapper objectMapper;

  @SuppressWarnings("EI_EXPOSE_REP2") // Spring-managed persistence collaborators are retained.
  public JdbcJobRepositoryAdapter(
      JdbcTemplate jdbc, CareerUserJdbcSupport users, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.users = users;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<Job> findBySourceIdentity(UserId userId, String source, String externalJobId) {
    return jdbc
        .query(
            "SELECT * FROM job WHERE user_id=? AND source=? AND external_job_id=?",
            this::mapJob,
            userId.value(),
            source,
            externalJobId)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Job> findByContentIdentity(
      UserId userId,
      String normalizedCompany,
      String normalizedTitle,
      String city,
      String contentHash) {
    return jdbc
        .query(
            """
            SELECT * FROM job
            WHERE user_id=? AND normalized_company=? AND normalized_title=?
              AND city IS NOT DISTINCT FROM ? AND content_hash=?
            ORDER BY first_seen_at,id LIMIT 1
            """,
            this::mapJob,
            userId.value(),
            normalizedCompany,
            normalizedTitle,
            city,
            contentHash)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Job> findById(UserId userId, UUID jobId) {
    return jdbc
        .query("SELECT * FROM job WHERE user_id=? AND id=?", this::mapJob, userId.value(), jobId)
        .stream()
        .findFirst();
  }

  @Override
  @Transactional
  public Job save(Job job) {
    users.ensure(job.userId());
    jdbc.update(
        """
        INSERT INTO job(
          id,user_id,source,external_job_id,title,normalized_title,company,normalized_company,
          city,salary_min,salary_max,salary_months,experience_min,experience_max,
          education_requirement,raw_jd,normalized_jd,publish_time,recruiter_activity,source_url,
          source_status,content_hash,metadata_json,first_seen_at,last_seen_at,created_at,updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CAST(? AS jsonb),?,?,?,?)
        ON CONFLICT (user_id,source,external_job_id) DO UPDATE SET
          title=EXCLUDED.title,normalized_title=EXCLUDED.normalized_title,
          company=EXCLUDED.company,normalized_company=EXCLUDED.normalized_company,
          city=EXCLUDED.city,salary_min=EXCLUDED.salary_min,salary_max=EXCLUDED.salary_max,
          salary_months=EXCLUDED.salary_months,experience_min=EXCLUDED.experience_min,
          experience_max=EXCLUDED.experience_max,education_requirement=EXCLUDED.education_requirement,
          raw_jd=EXCLUDED.raw_jd,normalized_jd=EXCLUDED.normalized_jd,publish_time=EXCLUDED.publish_time,
          recruiter_activity=EXCLUDED.recruiter_activity,source_url=EXCLUDED.source_url,
          source_status=EXCLUDED.source_status,content_hash=EXCLUDED.content_hash,
          metadata_json=EXCLUDED.metadata_json,last_seen_at=EXCLUDED.last_seen_at,
          updated_at=EXCLUDED.updated_at
        """,
        job.id(),
        job.userId().value(),
        job.source().name(),
        job.externalJobId(),
        job.title(),
        job.normalizedTitle(),
        job.company(),
        job.normalizedCompany(),
        job.city(),
        job.salaryMin(),
        job.salaryMax(),
        job.salaryMonths(),
        job.experienceMin(),
        job.experienceMax(),
        job.educationRequirement(),
        job.rawJd(),
        job.normalizedJd(),
        timestamp(job.publishTime()),
        job.recruiterActivity().name(),
        job.sourceUrl().toString(),
        job.sourceStatus().name(),
        job.contentHash(),
        json(job.metadata()),
        Timestamp.from(job.firstSeenAt()),
        Timestamp.from(job.lastSeenAt()),
        Timestamp.from(job.createdAt()),
        Timestamp.from(job.updatedAt()));
    return findBySourceIdentity(job.userId(), job.source().name(), job.externalJobId())
        .orElseThrow();
  }

  @Override
  public Optional<SourceSnapshot> findByJobAndContentHash(UUID jobId, String contentHash) {
    return jdbc
        .query(
            "SELECT * FROM job_source_snapshot WHERE job_id=? AND content_hash=?",
            this::mapSnapshot,
            jobId,
            contentHash)
        .stream()
        .findFirst();
  }

  @Override
  public SourceSnapshot save(SourceSnapshot snapshot) {
    jdbc.update(
        """
        INSERT INTO job_source_snapshot(
          id,job_id,search_id,source,external_job_id,source_url,raw_payload,content_hash,
          provider,fetched_at,parse_status,failure_code)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ON CONFLICT (job_id,content_hash) DO NOTHING
        """,
        snapshot.id(),
        snapshot.jobId(),
        snapshot.searchId(),
        snapshot.source().name(),
        snapshot.externalJobId(),
        snapshot.sourceUrl().toString(),
        snapshot.rawPayload(),
        snapshot.contentHash(),
        snapshot.provider().name(),
        Timestamp.from(snapshot.fetchedAt()),
        snapshot.parseStatus().name(),
        snapshot.failureCode());
    return findByJobAndContentHash(snapshot.jobId(), snapshot.contentHash()).orElseThrow();
  }

  @SuppressWarnings(
      "PMD.UnusedFormalParameter") // JdbcTemplate RowMapper contract supplies row number.
  private Job mapJob(ResultSet row, int number) throws SQLException {
    return new Job(
        row.getObject("id", UUID.class),
        new UserId(row.getObject("user_id", UUID.class)),
        Source.valueOf(row.getString("source")),
        row.getString("external_job_id"),
        row.getString("title"),
        row.getString("normalized_title"),
        row.getString("company"),
        row.getString("normalized_company"),
        row.getString("city"),
        integer(row, "salary_min"),
        integer(row, "salary_max"),
        integer(row, "salary_months"),
        integer(row, "experience_min"),
        integer(row, "experience_max"),
        row.getString("education_requirement"),
        row.getString("raw_jd"),
        row.getString("normalized_jd"),
        instant(row, "publish_time"),
        RecruiterActivity.valueOf(row.getString("recruiter_activity")),
        URI.create(row.getString("source_url")),
        SourceStatus.valueOf(row.getString("source_status")),
        row.getString("content_hash"),
        readMap(row.getString("metadata_json")),
        row.getTimestamp("first_seen_at").toInstant(),
        row.getTimestamp("last_seen_at").toInstant(),
        row.getTimestamp("created_at").toInstant(),
        row.getTimestamp("updated_at").toInstant());
  }

  @SuppressWarnings(
      "PMD.UnusedFormalParameter") // JdbcTemplate RowMapper contract supplies row number.
  private SourceSnapshot mapSnapshot(ResultSet row, int number) throws SQLException {
    return new SourceSnapshot(
        row.getObject("id", UUID.class),
        row.getObject("job_id", UUID.class),
        row.getObject("search_id", UUID.class),
        Source.valueOf(row.getString("source")),
        row.getString("external_job_id"),
        URI.create(row.getString("source_url")),
        row.getString("raw_payload"),
        row.getString("content_hash"),
        BrowserProviderType.valueOf(row.getString("provider")),
        row.getTimestamp("fetched_at").toInstant(),
        ParseStatus.valueOf(row.getString("parse_status")),
        row.getString("failure_code"));
  }

  private String json(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("岗位 metadata 无法序列化", exception);
    }
  }

  private Map<String, Object> readMap(String value) {
    try {
      return objectMapper.readValue(value, MAP_TYPE);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("岗位 metadata 无法读取", exception);
    }
  }

  private static Integer integer(ResultSet row, String column) throws SQLException {
    int value = row.getInt(column);
    return row.wasNull() ? null : value;
  }

  private static java.time.Instant instant(ResultSet row, String column) throws SQLException {
    Timestamp value = row.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private static Timestamp timestamp(java.time.Instant value) {
    return value == null ? null : Timestamp.from(value);
  }
}
