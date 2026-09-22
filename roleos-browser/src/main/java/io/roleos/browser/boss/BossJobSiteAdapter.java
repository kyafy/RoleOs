package io.roleos.browser.boss;

import io.roleos.browser.router.BrowserRouter;
import io.roleos.browser.router.BrowserRouter.BrowserTaskState;
import io.roleos.job.domain.JobTypes.SearchStep;
import io.roleos.job.port.JobSourcePort;
import io.roleos.job.port.JobSourcePort.FailureKind;
import io.roleos.job.port.JobSourcePort.JobSourceException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Boss JobSiteAdapter：只依赖 BrowserRouter 的结构化快照，不暴露页面句柄。 */
public final class BossJobSiteAdapter implements JobSourcePort {

  // Boss 搜索页将城市作为站点编码而非中文名称；该映射只收录已通过真实页面验证的值。
  // 未收录城市保留原查询值，由站点返回可观察的空结果/结构变更，不能猜测城市编码。
  private static final Map<String, String> VERIFIED_CITY_CODES = Map.of("杭州", "101210100");

  private final BrowserRouter router;
  private final BossSnapshotParser parser;

  public BossJobSiteAdapter(BrowserRouter router, BossSnapshotParser parser) {
    this.router = router;
    this.parser = parser;
  }

  @Override
  public List<RawJobSummary> search(JobSearchCriteria criteria, JobSourceContext context) {
    URI url = searchUrl(criteria);
    BrowserRouter.RoutedSnapshot routed;
    List<RawJobSummary> parsed;
    try {
      routed =
          router.navigate(
              new BrowserTaskState(context.searchId(), SearchStep.LIST_SNAPSHOT, url, "page=1"),
              context.requestedProvider());
      parsed = parser.parseList(routed.snapshot());
    } catch (io.roleos.browser.port.BrowserProvider.BrowserProviderException exception) {
      throw map(exception);
    }
    List<RawJobSummary> result = new ArrayList<>();
    for (RawJobSummary item : parsed) {
      Map<String, Object> metadata = new LinkedHashMap<>(item.rawData());
      metadata.put("provider", routed.provider().name());
      RawJobSummary enriched =
          new RawJobSummary(
              item.source(),
              item.externalJobId(),
              item.title(),
              item.company(),
              item.city(),
              item.salaryText(),
              item.experienceText(),
              item.educationText(),
              item.publishTime(),
              item.recruiterActivity(),
              item.sourceUrl(),
              Map.copyOf(metadata));
      result.add(enriched);
    }
    return List.copyOf(result);
  }

  @Override
  public RawJobDetail detail(RawJobSummary summary, JobSourceContext context) {
    BrowserRouter.RoutedSnapshot routed;
    RawJobDetail parsed;
    try {
      routed =
          router.navigate(
              new BrowserTaskState(
                  context.searchId(),
                  SearchStep.DETAIL_FETCH,
                  summary.sourceUrl(),
                  summary.externalJobId()),
              context.requestedProvider());
      parsed = parser.parseDetail(routed.snapshot(), summary);
    } catch (io.roleos.browser.port.BrowserProvider.BrowserProviderException exception) {
      throw map(exception);
    }
    return new RawJobDetail(
        parsed.summary(), parsed.rawJd(), parsed.rawData(), parsed.fetchedAt(), routed.provider());
  }

  static URI searchUrl(JobSearchCriteria criteria) {
    String query = String.join(" ", criteria.keywords());
    String city = VERIFIED_CITY_CODES.getOrDefault(criteria.city(), criteria.city());
    return URI.create(
        "https://www.zhipin.com/web/geek/job?query="
            + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&city="
            + URLEncoder.encode(city, StandardCharsets.UTF_8));
  }

  private static JobSourceException map(
      io.roleos.browser.port.BrowserProvider.BrowserProviderException exception) {
    FailureKind kind =
        switch (exception.category()) {
          case PAUSE_FOR_HUMAN -> FailureKind.HUMAN_REQUIRED;
          case TERMINAL_FAILURE -> FailureKind.TERMINAL;
          case RETRY_SAME_PROVIDER, SWITCH_PROVIDER -> FailureKind.RETRYABLE;
        };
    return new JobSourceException(kind, exception.reasonCode(), exception.getMessage());
  }
}
