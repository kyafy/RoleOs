package io.roleos.browser.boss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.roleos.browser.FakeBrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.PageSnapshot;
import io.roleos.browser.router.BrowserRouter;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.port.JobSourcePort.JobSearchCriteria;
import io.roleos.job.port.JobSourcePort.JobSourceContext;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 脱敏 Boss Snapshot Fixture 覆盖列表、详情、缺失字段、页面变化和 Captcha。 */
class BossJobSiteAdapterContractTest {

  private final BossSnapshotParser parser = new BossSnapshotParser();

  @Test
  void usesOnlyVerifiedBossCityCodesAndLeavesUnknownCitiesUntouched() {
    var hangzhou =
        BossJobSiteAdapter.searchUrl(
            new JobSearchCriteria(
                List.of("Java"), "杭州", null, null, "Java工程师", BrowserProviderType.KIMI_WEBBRIDGE));
    var unmapped =
        BossJobSiteAdapter.searchUrl(
            new JobSearchCriteria(
                List.of("Java"), "苏州", null, null, "Java工程师", BrowserProviderType.KIMI_WEBBRIDGE));

    assertThat(hangzhou.getRawQuery()).contains("city=101210100");
    assertThat(unmapped.getRawQuery()).contains("city=%E8%8B%8F%E5%B7%9E");
  }

  @Test
  void keepsInterleavedSearchProviderAndDetailContextIsolated() {
    String fixture =
        """
        JOB id=isolated-1 | title=AI Agent工程师 | company=示例公司 | city=上海 | url=https://www.zhipin.com/job_detail/isolated-1.html
        DETAIL id=isolated-1 | jd=负责 Agent 平台
        """;
    var kimi = new FakeBrowserProvider(BrowserProviderType.KIMI_WEBBRIDGE).content(fixture);
    var playwright = new FakeBrowserProvider(BrowserProviderType.PLAYWRIGHT_MCP).content(fixture);
    var adapter =
        new BossJobSiteAdapter(
            new BrowserRouter(
                List.of(kimi, playwright),
                BrowserProviderType.KIMI_WEBBRIDGE,
                BrowserProviderType.PLAYWRIGHT_MCP,
                1),
            parser);
    var criteria =
        new JobSearchCriteria(
            List.of("AI Agent"),
            "上海",
            null,
            null,
            "AI Agent 工程师",
            BrowserProviderType.KIMI_WEBBRIDGE);
    var firstContext =
        new JobSourceContext(UUID.randomUUID(), BrowserProviderType.KIMI_WEBBRIDGE, null, null);
    var first = adapter.search(criteria, firstContext).getFirst();
    adapter.search(
        new JobSearchCriteria(
            List.of("Java"), "杭州", null, null, "Java工程师", BrowserProviderType.PLAYWRIGHT_MCP),
        new JobSourceContext(UUID.randomUUID(), BrowserProviderType.PLAYWRIGHT_MCP, null, null));

    var detail = adapter.detail(first, firstContext);

    assertThat(detail.provider()).isEqualTo(BrowserProviderType.KIMI_WEBBRIDGE);
    assertThat(kimi.calls()).containsExactly("open", "navigate", "open", "navigate");
    assertThat(playwright.calls()).containsExactly("open", "navigate");
  }

  @Test
  void parsesListAndDetailWhileKeepingMissingFieldsUnknown() {
    var jobs =
        parser.parseList(
            page(
                """
        heading Boss职位
        JOB id=boss-1 | title=AI Agent工程师 | company=示例公司 | city=上海 | salary=20-30K·14薪 | experience=3-5年 | education=本科 | published=2026-09-19T00:00:00Z | activity=TODAY | url=https://www.zhipin.com/job_detail/boss-1.html
        JOB id=boss-2 | title=Java工程师 | company=另一公司 | url=https://www.zhipin.com/job_detail/boss-2.html
        """));

    assertThat(jobs).hasSize(2);
    assertThat(jobs.get(1).salaryText()).isNull();
    assertThat(jobs.get(1).recruiterActivity()).isEqualTo(RecruiterActivity.UNKNOWN);
    var detail =
        parser.parseDetail(page("DETAIL id=boss-1 | jd=负责 Java 与 Agent 平台"), jobs.getFirst());
    assertThat(detail.rawJd()).isEqualTo("负责 Java 与 Agent 平台");
  }

  @Test
  void parsesObservedAccessibilityShapeWithoutInventingSalaryOrKeepingBrowserReferences() {
    var jobs =
        parser.parseList(
            page(
                """
        {"tree":[{"role":"list","children":[{"role":"listitem","children":[
          {"role":"link","name":"Java工程师","ref":"@e21","children":[{"role":"StaticText","name":"Java工程师"}]},
          {"role":"StaticText","name":"-K"},
          {"role":"list","children":[{"role":"listitem","children":[{"role":"StaticText","name":"5-10年"}]},{"role":"listitem","children":[{"role":"StaticText","name":"本科"}]}]},
          {"role":"link","name":"测试夹具公司","ref":"@e22","children":[{"role":"StaticText","name":"测试夹具公司"}]},
          {"role":"StaticText","name":"杭州·西湖区·西溪"}
        ]}]}],"links":[{"name":"Java工程师","url":"https://www.zhipin.com/job_detail/fixture-1.html"}]}
        """));
    assertThat(jobs).hasSize(1);
    var summary = jobs.getFirst();
    assertThat(summary.externalJobId()).isEqualTo("fixture-1");
    assertThat(summary.city()).isEqualTo("杭州");
    assertThat(summary.experienceText()).isEqualTo("5-10年");
    assertThat(summary.educationText()).isEqualTo("本科");
    assertThat(summary.publishTime()).isNull();
    assertThat(summary.recruiterActivity()).isEqualTo(RecruiterActivity.UNKNOWN);
    assertThat(summary.rawData().toString()).doesNotContain("@e21", "@e22");
    var detail =
        parser.parseDetail(
            page(
                """
        {"tree":[{"role":"heading","name":"职位描述"},
          {"role":"paragraph","children":[{"role":"StaticText","name":"负责Java","children":[{"role":"InlineTextBox","name":"负责Java"}]},{"role":"StaticText","name":"后端开发"}]},
          {"role":"heading","name":"招聘方"},{"role":"paragraph","children":[{"role":"StaticText","name":"不属于JD"}]}],"links":[]}
        """),
            summary);
    assertThat(detail.rawJd()).isEqualTo("负责Java后端开发");
    var job =
        new io.roleos.job.service.JobNormalizationService(java.time.Clock.systemUTC())
            .normalize(io.roleos.domain.career.UserId.random(), detail);
    assertThat(job.salaryMin()).isNull();
    assertThat(job.salaryMax()).isNull();
  }

  @Test
  void usesCardScopedPublicFactsAndRequiresDetailCompanyBeforeNormalization() {
    var summary =
        parser
            .parseList(
                page(
                    """
        {"tree":[],"cards":[{"title":"Java工程师","url":"https://www.zhipin.com/job_detail/card-1.html","salary":"面议","tags":["5-10年","本科"],"location":"杭州·西湖区"}],"detail":{"company":null,"jd":null}}
        """))
            .getFirst();

    assertThat(summary.company()).isNull();
    assertThat(summary.city()).isEqualTo("杭州");
    assertThat(summary.experienceText()).isEqualTo("5-10年");
    var detail =
        parser.parseDetail(
            page(
                """
        {"tree":[],"cards":[],"detail":{"company":"测试夹具公司","jd":"负责 Java 服务"}}
        """),
            summary);
    assertThat(detail.summary().company()).isEqualTo("测试夹具公司");
    assertThat(detail.rawJd()).isEqualTo("负责 Java 服务");
  }

  @Test
  void pageChangeAndCaptchaReturnSafeClassifications() {
    assertThatThrownBy(() -> parser.parseList(page("unknown new layout")))
        .isInstanceOfSatisfying(
            BrowserProviderException.class,
            exception ->
                assertThat(exception.reasonCode()).isEqualTo("BOSS_LIST_EMPTY_OR_CHANGED"));
    assertThatThrownBy(() -> parser.parseList(page("请完成验证码后继续")))
        .isInstanceOfSatisfying(
            BrowserProviderException.class,
            exception ->
                assertThat(exception.category())
                    .isEqualTo(
                        io.roleos.browser.port.BrowserProvider.FailureCategory.PAUSE_FOR_HUMAN));
  }

  private PageSnapshot page(String text) {
    return new PageSnapshot(
        UUID.randomUUID(),
        URI.create("https://www.zhipin.com/web/geek/job"),
        text,
        List.of(),
        Instant.parse("2026-09-19T09:00:00Z"));
  }
}
