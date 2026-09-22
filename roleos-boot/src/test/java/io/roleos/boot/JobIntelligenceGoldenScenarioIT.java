package io.roleos.boot;

import static org.assertj.core.api.Assertions.assertThat;

import io.roleos.browser.port.AbstractBrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.browser.port.BrowserProvider.PageSnapshot;
import io.roleos.browser.port.BrowserProvider.ProviderContext;
import io.roleos.browser.port.BrowserProvider.ProviderElementRef;
import io.roleos.browser.router.BrowserRouter;
import io.roleos.browser.router.BrowserRouter.BrowserTaskState;
import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.SearchState;
import io.roleos.job.domain.JobTypes.SearchStep;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobSearchRepository;
import io.roleos.job.port.JobSourcePort;
import io.roleos.job.port.JobSourcePort.JobSearchCriteria;
import io.roleos.job.port.JobSourcePort.RawJobDetail;
import io.roleos.job.port.JobSourcePort.RawJobSummary;
import io.roleos.job.port.SemanticRankingPort.RankingContext;
import io.roleos.job.service.JobSearchOrchestrator;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

/** Feature 002 Golden Scenario：PostgreSQL 业务闭环与 Provider 切换后的重新导航。 */
class JobIntelligenceGoldenScenarioIT {

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @BeforeAll
  static void startPostgres() {
    POSTGRES.start();
  }

  @AfterAll
  static void stopPostgres() {
    POSTGRES.stop();
  }

  @Test
  void persistsGoldenSearchAndRestoresItAfterRestart() {
    UserId userId = UserId.random();
    UUID searchId;
    UUID jobId;

    try (ConfigurableApplicationContext first = startApplication()) {
      JobSearchOrchestrator orchestrator = first.getBean(JobSearchOrchestrator.class);
      var completed =
          orchestrator.start(
              userId,
              new JobSearchCriteria(
                  List.of("AI Agent"), "上海", 25, 5, "AI Agent 工程师", BrowserProviderType.FAKE),
              new RankingContext("AI Agent 工程师", List.of("Java", "Spring Boot"), "Agent 工程"));

      assertThat(completed.state()).isEqualTo(SearchState.COMPLETED);
      assertThat(completed.activeProvider()).isEqualTo(BrowserProviderType.FAKE);
      searchId = completed.id();
      var candidates =
          first.getBean(JobCandidateRepository.class).findAll(userId, null, null, 0, 10);
      assertThat(candidates).singleElement();
      jobId = candidates.getFirst().jobId();
      assertThat(candidates.getFirst().rankingScore()).isNotNull();
    }

    try (ConfigurableApplicationContext second = startApplication()) {
      var restored = second.getBean(JobSearchRepository.class).find(userId, searchId);
      assertThat(restored).isPresent().get().extracting("state").isEqualTo(SearchState.COMPLETED);
      assertThat(second.getBean(JobCandidateRepository.class).find(userId, jobId)).isPresent();
    }
  }

  @Test
  void playwrightFailureSwitchesToKimiAndRenavigatesFromBusinessUrl() {
    TestProvider primary = new TestProvider(BrowserProviderType.PLAYWRIGHT_MCP, true);
    TestProvider fallback = new TestProvider(BrowserProviderType.KIMI_WEBBRIDGE, false);
    BrowserRouter router =
        new BrowserRouter(
            List.of(primary, fallback),
            BrowserProviderType.PLAYWRIGHT_MCP,
            BrowserProviderType.KIMI_WEBBRIDGE,
            1);
    URI resumeUrl = URI.create("https://www.zhipin.com/web/geek/job?query=AI%20Agent");

    var routed =
        router.navigate(
            new BrowserTaskState(UUID.randomUUID(), SearchStep.LIST_SNAPSHOT, resumeUrl, "page=1"),
            BrowserProviderType.AUTO);

    assertThat(routed.provider()).isEqualTo(BrowserProviderType.KIMI_WEBBRIDGE);
    assertThat(primary.calls).containsExactly("open", "navigate", "close");
    assertThat(fallback.calls).containsExactly("open", "navigate");
    assertThat(fallback.lastUrl).isEqualTo(resumeUrl);
  }

  private ConfigurableApplicationContext startApplication() {
    return new SpringApplicationBuilder(RoleOsApplication.class, GoldenSourceConfiguration.class)
        .registerShutdownHook(false)
        .run(
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword(),
            "--server.port=0",
            "--spring.jmx.enabled=false");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class GoldenSourceConfiguration {
    @Bean
    @Primary
    JobSourcePort goldenJobSource() {
      return new GoldenJobSource();
    }
  }

  private static final class GoldenJobSource implements JobSourcePort {
    private static final URI URL = URI.create("https://www.zhipin.com/job_detail/golden-job.html");

    @Override
    public List<RawJobSummary> search(
        JobSearchCriteria criteria, JobSourcePort.JobSourceContext context) {
      return List.of(
          new RawJobSummary(
              Source.BOSS,
              "golden-job",
              "AI Agent 工程师",
              "Fixture 公司",
              "上海",
              "30-50K·14薪",
              "3-5年",
              "本科",
              Instant.parse("2026-09-19T07:00:00Z"),
              RecruiterActivity.TODAY,
              URL,
              Map.of("fixture", true)));
    }

    @Override
    public RawJobDetail detail(RawJobSummary summary, JobSourcePort.JobSourceContext context) {
      return new RawJobDetail(
          summary,
          "负责 Java、Spring Boot 与 AI Agent 平台研发。",
          Map.of("fixture", true),
          Instant.parse("2026-09-19T08:00:00Z"),
          BrowserProviderType.FAKE);
    }
  }

  private static final class TestProvider extends AbstractBrowserProvider {
    private final BrowserProviderType type;
    private final boolean fail;
    private final List<String> calls = new ArrayList<>();
    private URI lastUrl;

    private TestProvider(BrowserProviderType type, boolean fail) {
      this.type = type;
      this.fail = fail;
    }

    @Override
    public BrowserProviderType type() {
      return type;
    }

    @Override
    public ProviderContext open(Map<String, String> safeConfiguration) {
      calls.add("open");
      return new ProviderContext(UUID.randomUUID(), type);
    }

    @Override
    public PageSnapshot navigate(ProviderContext context, URI url) {
      calls.add("navigate");
      if (fail) {
        throw new BrowserProviderException(
            FailureCategory.SWITCH_PROVIDER, "FIXTURE_SWITCH", "fixture failure");
      }
      lastUrl = url;
      return new PageSnapshot(
          context.providerInstanceId(), url, "fixture", List.of(), Instant.now());
    }

    @Override
    public PageSnapshot snapshot(ProviderContext context) {
      calls.add("snapshot");
      return new PageSnapshot(
          context.providerInstanceId(), lastUrl, "fixture", List.of(), Instant.now());
    }

    @Override
    public PageSnapshot click(ProviderContext context, ProviderElementRef element) {
      return snapshot(context);
    }

    @Override
    public PageSnapshot fill(ProviderContext context, ProviderElementRef element, String value) {
      return snapshot(context);
    }

    @Override
    protected void closeInternal(ProviderContext context) {
      calls.add("close");
    }
  }
}
