package io.roleos.storage.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobSearch;
import io.roleos.job.domain.JobSearch.SourceSnapshot;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.ParseStatus;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.SearchState;
import io.roleos.job.domain.JobTypes.SearchStep;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.storage.PostgreSqlIntegrationSupport;
import io.roleos.storage.career.CareerUserJdbcSupport;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** V7 Job 与 Snapshot 约束的 PostgreSQL 集成验证。 */
class JdbcJobRepositoryAdapterIT extends PostgreSqlIntegrationSupport {

  private static final Instant NOW = Instant.parse("2026-09-19T04:00:00Z");
  private static final String HASH = "a".repeat(64);

  private JdbcTemplate jdbc;
  private JdbcJobRepositoryAdapter repository;

  @BeforeEach
  void setUp() {
    DataSource dataSource = migrateSchema().getConfiguration().getDataSource();
    jdbc = new JdbcTemplate(dataSource);
    repository =
        new JdbcJobRepositoryAdapter(jdbc, new CareerUserJdbcSupport(jdbc), new ObjectMapper());
  }

  @Test
  void upsertsBySourceIdentityAndKeepsSingleSnapshotPerHash() {
    UserId userId = UserId.random();
    Job original = job(userId, UUID.randomUUID(), "初始标题", HASH);
    Job saved = repository.save(original);
    Job updated = job(userId, UUID.randomUUID(), "更新标题", "b".repeat(64));

    Job afterUpdate = repository.save(updated);

    assertThat(afterUpdate.id()).isEqualTo(saved.id());
    assertThat(afterUpdate.title()).isEqualTo("更新标题");
    assertThat(repository.findBySourceIdentity(userId, "BOSS", "boss-1001")).isPresent();

    UUID searchId = insertSearch(userId);
    SourceSnapshot first = snapshot(afterUpdate.id(), searchId, "b".repeat(64));
    SourceSnapshot duplicate =
        new SourceSnapshot(
            UUID.randomUUID(),
            afterUpdate.id(),
            searchId,
            Source.BOSS,
            "boss-1001",
            URI.create("https://www.zhipin.com/job_detail/boss-1001.html"),
            "{\"same\":true}",
            "b".repeat(64),
            BrowserProviderType.FAKE,
            NOW,
            ParseStatus.SUCCEEDED,
            null);

    repository.save(first);
    SourceSnapshot savedDuplicate = repository.save(duplicate);

    assertThat(savedDuplicate.id()).isEqualTo(first.id());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM job_source_snapshot WHERE job_id=?",
                Integer.class,
                afterUpdate.id()))
        .isEqualTo(1);
  }

  @Test
  void neverReturnsAnotherUsersJob() {
    UserId owner = UserId.random();
    UserId other = UserId.random();
    Job saved = repository.save(job(owner, UUID.randomUUID(), "AI Agent", HASH));

    assertThat(repository.findById(owner, saved.id())).isPresent();
    assertThat(repository.findById(other, saved.id())).isEmpty();
    assertThat(
            repository.findByContentIdentity(
                other, saved.normalizedCompany(), saved.normalizedTitle(), saved.city(), HASH))
        .isEmpty();
  }

  @Test
  void persistsSearchBusinessStateWithoutProviderHandles() {
    UserId owner = UserId.random();
    JdbcJobSearchRepositoryAdapter searches =
        new JdbcJobSearchRepositoryAdapter(
            jdbc, new CareerUserJdbcSupport(jdbc), new ObjectMapper());
    JobSearch search =
        new JobSearch(
            UUID.randomUUID(),
            owner,
            Map.of("keywords", java.util.List.of("AI Agent"), "city", "上海"),
            "search-command-001",
            null,
            BrowserProviderType.AUTO,
            BrowserProviderType.PLAYWRIGHT_MCP,
            SearchState.RUNNING,
            SearchStep.DETAIL_FETCH,
            URI.create("https://www.zhipin.com/web/geek/job"),
            "boss-1001",
            null,
            null,
            1,
            new JobSearch.Counts(3, 2, 1, 1),
            NOW,
            NOW,
            null);

    JobSearch saved = searches.save(search);

    assertThat(saved.criteria()).containsEntry("city", "上海");
    assertThat(saved.step()).isEqualTo(SearchStep.DETAIL_FETCH);
    assertThat(saved.counts()).isEqualTo(new JobSearch.Counts(3, 2, 1, 1));
    assertThat(searches.findByStartCommandId(owner, "search-command-001")).contains(saved);
    assertThat(searches.find(UserId.random(), saved.id())).isEmpty();
  }

  private UUID insertSearch(UserId userId) {
    new CareerUserJdbcSupport(jdbc).ensure(userId);
    UUID searchId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO job_search(id,user_id,requested_provider,state,step,started_at,updated_at) VALUES (?,?,?,?,?,?,?)",
        searchId,
        userId.value(),
        "AUTO",
        "RUNNING",
        "DETAIL_FETCH",
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    return searchId;
  }

  private Job job(UserId userId, UUID id, String title, String contentHash) {
    return new Job(
        id,
        userId,
        Source.BOSS,
        "boss-1001",
        title,
        title.toLowerCase(),
        "示例公司",
        "示例公司",
        "上海",
        20_000,
        30_000,
        14,
        3,
        5,
        "本科",
        "负责 AI Agent 平台",
        "负责 AI Agent 平台",
        NOW,
        RecruiterActivity.TODAY,
        URI.create("https://www.zhipin.com/job_detail/boss-1001.html"),
        SourceStatus.ACTIVE,
        contentHash,
        Map.of("fixture", true),
        NOW,
        NOW,
        NOW,
        NOW);
  }

  private SourceSnapshot snapshot(UUID jobId, UUID searchId, String hash) {
    return new SourceSnapshot(
        UUID.randomUUID(),
        jobId,
        searchId,
        Source.BOSS,
        "boss-1001",
        URI.create("https://www.zhipin.com/job_detail/boss-1001.html"),
        "{\"fixture\":true}",
        hash,
        BrowserProviderType.FAKE,
        NOW,
        ParseStatus.SUCCEEDED,
        null);
  }
}
