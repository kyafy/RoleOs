package io.roleos.job.domain;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 外部岗位事实的当前 Canonical Snapshot；不得承载用户求职策略或系统推荐。 */
public record Job(
    UUID id,
    UserId userId,
    Source source,
    String externalJobId,
    String title,
    String normalizedTitle,
    String company,
    String normalizedCompany,
    String city,
    Integer salaryMin,
    Integer salaryMax,
    Integer salaryMonths,
    Integer experienceMin,
    Integer experienceMax,
    String educationRequirement,
    String rawJd,
    String normalizedJd,
    Instant publishTime,
    RecruiterActivity recruiterActivity,
    URI sourceUrl,
    SourceStatus sourceStatus,
    String contentHash,
    Map<String, Object> metadata,
    Instant firstSeenAt,
    Instant lastSeenAt,
    Instant createdAt,
    Instant updatedAt) {

  public Job {
    Objects.requireNonNull(id, "岗位标识不能为空");
    Objects.requireNonNull(userId, "用户不能为空");
    Objects.requireNonNull(source, "岗位来源不能为空");
    requireText(externalJobId, "外部岗位标识不能为空");
    requireText(title, "岗位标题不能为空");
    requireText(normalizedTitle, "标准化岗位标题不能为空");
    requireText(company, "公司不能为空");
    requireText(normalizedCompany, "标准化公司不能为空");
    requireText(rawJd, "原始 JD 不能为空");
    requireText(normalizedJd, "标准化 JD 不能为空");
    validateRange(salaryMin, salaryMax, "薪资");
    if (salaryMonths != null && salaryMonths <= 0) {
      throw new IllegalArgumentException("薪资月数必须大于 0");
    }
    validateExperience(experienceMin, experienceMax);
    Objects.requireNonNull(recruiterActivity, "招聘方活跃度不能为空");
    validateSourceUrl(sourceUrl, source);
    Objects.requireNonNull(sourceStatus, "来源状态不能为空");
    if (contentHash == null || !contentHash.matches("[a-f0-9]{64}")) {
      throw new IllegalArgumentException("内容摘要必须是小写 SHA-256");
    }
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    Objects.requireNonNull(firstSeenAt, "首次发现时间不能为空");
    Objects.requireNonNull(lastSeenAt, "最近发现时间不能为空");
    Objects.requireNonNull(createdAt, "创建时间不能为空");
    Objects.requireNonNull(updatedAt, "更新时间不能为空");
    if (lastSeenAt.isBefore(firstSeenAt)) {
      throw new IllegalArgumentException("最近发现时间不能早于首次发现时间");
    }
  }

  private static void validateSourceUrl(URI sourceUrl, Source source) {
    Objects.requireNonNull(sourceUrl, "来源链接不能为空");
    if (!"https".equalsIgnoreCase(sourceUrl.getScheme())) {
      throw new IllegalArgumentException("来源链接必须使用 HTTPS");
    }
    String host = sourceUrl.getHost();
    if (source == Source.BOSS
        && (host == null || !("zhipin.com".equals(host) || host.endsWith(".zhipin.com")))) {
      throw new IllegalArgumentException("Boss 岗位链接必须使用受信 zhipin.com 域名");
    }
  }

  private static void validateRange(Integer minimum, Integer maximum, String label) {
    if (minimum != null && minimum < 0 || maximum != null && maximum < 0) {
      throw new IllegalArgumentException(label + "不能为负数");
    }
    if (minimum != null && maximum != null && minimum > maximum) {
      throw new IllegalArgumentException(label + "最小值不能大于最大值");
    }
  }

  private static void validateExperience(Integer minimum, Integer maximum) {
    validateRange(minimum, maximum, "经验年限");
    if (minimum != null && minimum > 60 || maximum != null && maximum > 60) {
      throw new IllegalArgumentException("经验年限必须在 0..60 范围内");
    }
  }

  private static void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
