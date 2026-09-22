package io.roleos.job.port;

import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 招聘来源端口；调用者只能获得来源事实，不能接触 Browser Provider 私有对象。 */
public interface JobSourcePort {

  /** 使用持久化搜索上下文发现列表，不得在 Adapter 中保留跨搜索的可变上下文。 */
  List<RawJobSummary> search(JobSearchCriteria criteria, JobSourceContext context);

  /** 根据刚刚读取的来源事实获取详情，调用方不传递或存储 Provider 私有句柄。 */
  RawJobDetail detail(RawJobSummary summary, JobSourceContext context);

  enum FailureKind {
    RETRYABLE,
    HUMAN_REQUIRED,
    TERMINAL
  }

  /** 招聘来源边界上的安全失败；不得携带 DOM、凭据或 Provider 私有状态。 */
  final class JobSourceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final FailureKind failureKind;
    private final String safeReasonCode;

    public JobSourceException(FailureKind kind, String reasonCode, String safeMessage) {
      super(safeMessage);
      this.failureKind = java.util.Objects.requireNonNull(kind);
      this.safeReasonCode = java.util.Objects.requireNonNull(reasonCode);
    }

    public FailureKind kind() {
      return failureKind;
    }

    public String reasonCode() {
      return safeReasonCode;
    }
  }

  record JobSearchCriteria(
      List<String> keywords,
      String city,
      Integer salaryMin,
      Integer experienceYears,
      String targetRole,
      BrowserProviderType provider) {
    public JobSearchCriteria {
      keywords = keywords == null ? List.of() : List.copyOf(keywords);
      if (keywords.isEmpty() || keywords.size() > 5) {
        throw new IllegalArgumentException("搜索关键词数量必须在 1..5 之间");
      }
      if (city == null || city.isBlank()) throw new IllegalArgumentException("搜索城市不能为空");
      if (salaryMin != null && salaryMin < 0) throw new IllegalArgumentException("最低薪资不能为负数");
      if (experienceYears != null && (experienceYears < 0 || experienceYears > 60)) {
        throw new IllegalArgumentException("经验年限必须在 0..60 之间");
      }
      if (targetRole == null || targetRole.isBlank()) {
        throw new IllegalArgumentException("目标岗位不能为空");
      }
      if (provider == null) provider = BrowserProviderType.AUTO;
    }
  }

  /** 可恢复的跨 Adapter 搜索上下文；不包含 Cookie、DOM Ref 或浏览器 Session。 */
  record JobSourceContext(
      UUID searchId, BrowserProviderType requestedProvider, URI resumeUrl, String cursor) {
    public JobSourceContext {
      Objects.requireNonNull(searchId, "搜索标识不能为空");
      requestedProvider = requestedProvider == null ? BrowserProviderType.AUTO : requestedProvider;
      if (resumeUrl != null && !"https".equalsIgnoreCase(resumeUrl.getScheme())) {
        throw new IllegalArgumentException("搜索恢复链接必须使用 HTTPS");
      }
    }
  }

  record RawJobSummary(
      Source source,
      String externalJobId,
      String title,
      String company,
      String city,
      String salaryText,
      String experienceText,
      String educationText,
      Instant publishTime,
      RecruiterActivity recruiterActivity,
      URI sourceUrl,
      Map<String, Object> rawData) {
    public RawJobSummary {
      rawData = rawData == null ? Map.of() : Map.copyOf(rawData);
    }
  }

  @SuppressWarnings("EI_EXPOSE_REP") // Compact constructor stores an immutable defensive copy.
  record RawJobDetail(
      RawJobSummary summary,
      String rawJd,
      Map<String, Object> rawData,
      Instant fetchedAt,
      BrowserProviderType provider) {
    public RawJobDetail {
      rawData = rawData == null ? Map.of() : Map.copyOf(rawData);
    }
  }
}
