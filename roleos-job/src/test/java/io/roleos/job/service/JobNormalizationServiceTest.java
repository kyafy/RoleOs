package io.roleos.job.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.port.JobSourcePort.RawJobDetail;
import io.roleos.job.port.JobSourcePort.RawJobSummary;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Job 标准化必须保留原始事实，并对缺失字段保持 Unknown。 */
class JobNormalizationServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-19T03:30:00Z");
  private static final UserId USER_ID = UserId.random();

  private final JobNormalizationService service =
      new JobNormalizationService(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void normalizesStructuredFieldsWithoutRewritingJdFacts() {
    RawJobSummary summary =
        new RawJobSummary(
            Source.BOSS,
            "boss-1001",
            "  AI   Agent 工程师 ",
            " 示例 科技 ",
            "上海",
            "25-35K·14薪",
            "3-5年",
            "本科",
            NOW.minusSeconds(3600),
            RecruiterActivity.TODAY,
            URI.create("https://www.zhipin.com/job_detail/boss-1001.html"),
            Map.of("source", "fixture"));
    RawJobDetail detail =
        new RawJobDetail(
            summary,
            "  负责 MCP  平台建设。\n\n不得虚构生产经验。 ",
            Map.of("detail", true),
            NOW,
            BrowserProviderType.FAKE);

    var job = service.normalize(USER_ID, detail);

    assertThat(job.title()).isEqualTo("AI Agent 工程师");
    assertThat(job.normalizedTitle()).isEqualTo("ai agent 工程师");
    assertThat(job.company()).isEqualTo("示例 科技");
    assertThat(job.salaryMin()).isEqualTo(25_000);
    assertThat(job.salaryMax()).isEqualTo(35_000);
    assertThat(job.salaryMonths()).isEqualTo(14);
    assertThat(job.experienceMin()).isEqualTo(3);
    assertThat(job.experienceMax()).isEqualTo(5);
    assertThat(job.rawJd()).startsWith("  负责 MCP");
    assertThat(job.normalizedJd()).isEqualTo("负责 MCP 平台建设。\n不得虚构生产经验。");
    assertThat(job.contentHash()).matches("[a-f0-9]{64}");
    assertThat(job.firstSeenAt()).isEqualTo(NOW);
  }

  @Test
  void keepsMissingAndAmbiguousFieldsUnknown() {
    RawJobSummary summary =
        new RawJobSummary(
            Source.BOSS,
            "boss-unknown",
            "AI 工程师",
            "示例公司",
            null,
            "面议",
            "经验不限",
            null,
            null,
            RecruiterActivity.UNKNOWN,
            URI.create("https://www.zhipin.com/job_detail/boss-unknown.html"),
            Map.of());

    var job =
        service.normalize(
            USER_ID,
            new RawJobDetail(summary, "负责 AI 应用", Map.of(), NOW, BrowserProviderType.FAKE));

    assertThat(job.city()).isNull();
    assertThat(job.salaryMin()).isNull();
    assertThat(job.salaryMax()).isNull();
    assertThat(job.salaryMonths()).isNull();
    assertThat(job.experienceMin()).isNull();
    assertThat(job.experienceMax()).isNull();
    assertThat(job.educationRequirement()).isNull();
    assertThat(job.publishTime()).isNull();
  }

  @Test
  void producesStableHashForEquivalentWhitespace() {
    RawJobSummary summary = baseSummary();
    RawJobDetail first =
        new RawJobDetail(summary, "Java  MCP\nRAG", Map.of(), NOW, BrowserProviderType.FAKE);
    RawJobDetail second =
        new RawJobDetail(summary, "Java MCP\n\nRAG", Map.of(), NOW, BrowserProviderType.FAKE);

    assertThat(service.normalize(USER_ID, first).contentHash())
        .isEqualTo(service.normalize(USER_ID, second).contentHash());
  }

  private RawJobSummary baseSummary() {
    return new RawJobSummary(
        Source.BOSS,
        "boss-hash",
        "Agent Engineer",
        "RoleOS",
        "上海",
        "20-30K",
        "3-5年",
        "本科",
        NOW,
        RecruiterActivity.RECENT,
        URI.create("https://www.zhipin.com/job_detail/boss-hash.html"),
        Map.of());
  }
}
