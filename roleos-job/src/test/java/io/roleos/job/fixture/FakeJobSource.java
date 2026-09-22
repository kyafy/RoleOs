package io.roleos.job.fixture;

import io.roleos.job.port.JobSourcePort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 测试用 Job Source，可按 externalJobId 编排详情或失败。 */
public final class FakeJobSource implements JobSourcePort {

  private final List<RawJobSummary> summaries;
  private final Map<String, RawJobDetail> details = new LinkedHashMap<>();
  private final Map<String, RuntimeException> failures = new LinkedHashMap<>();

  public FakeJobSource(List<RawJobSummary> summaries) {
    this.summaries = List.copyOf(summaries);
  }

  public FakeJobSource detail(RawJobDetail detail) {
    details.put(detail.summary().externalJobId(), detail);
    return this;
  }

  public FakeJobSource fail(String externalJobId, RuntimeException exception) {
    failures.put(externalJobId, exception);
    return this;
  }

  @Override
  public List<RawJobSummary> search(JobSearchCriteria criteria, JobSourceContext context) {
    return summaries;
  }

  @Override
  public RawJobDetail detail(RawJobSummary summary, JobSourceContext context) {
    String externalJobId = summary.externalJobId();
    RuntimeException failure = failures.get(externalJobId);
    if (failure != null) throw failure;
    RawJobDetail detail = details.get(externalJobId);
    if (detail == null) throw new IllegalArgumentException("缺少 Fake 详情: " + externalJobId);
    return detail;
  }
}
