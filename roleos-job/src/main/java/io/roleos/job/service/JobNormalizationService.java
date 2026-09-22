package io.roleos.job.service;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.port.JobSourcePort.RawJobDetail;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 将招聘站点原始事实转换为 RoleOS Canonical Job，不推断缺失信息。 */
public final class JobNormalizationService {

  private static final Pattern SALARY_RANGE =
      Pattern.compile("(?i)(\\d+)\\s*[-~至]\\s*(\\d+)\\s*[kK]");
  private static final Pattern SALARY_MONTHS = Pattern.compile("(\\d+)\\s*薪");
  private static final Pattern EXPERIENCE_RANGE = Pattern.compile("(\\d+)\\s*[-~至]\\s*(\\d+)\\s*年");
  private static final Pattern EXPERIENCE_SINGLE = Pattern.compile("(\\d+)\\s*年");

  private final Clock clock;

  public JobNormalizationService(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock 不能为空");
  }

  /**
   * 标准化单个详情快照。原始 JD 原样保存；标准化 JD 只移除空行并折叠行内空白。
   *
   * @param userId 数据归属用户
   * @param detail 来源详情快照
   * @return 尚未持久化的 Canonical Job
   */
  public Job normalize(UserId userId, RawJobDetail detail) {
    Objects.requireNonNull(userId, "userId 不能为空");
    Objects.requireNonNull(detail, "detail 不能为空");
    var summary = Objects.requireNonNull(detail.summary(), "岗位摘要不能为空");
    Instant now = clock.instant();
    String title = normalizeInline(summary.title());
    String company = normalizeInline(summary.company());
    String normalizedJd = normalizeJd(detail.rawJd());
    Range salary = parseSalary(summary.salaryText());
    Range experience = parseExperience(summary.experienceText());

    return new Job(
        UUID.randomUUID(),
        userId,
        summary.source(),
        requireText(summary.externalJobId(), "外部岗位标识不能为空"),
        title,
        title.toLowerCase(Locale.ROOT),
        company,
        company.toLowerCase(Locale.ROOT),
        normalizeNullable(summary.city()),
        salary.minimum(),
        salary.maximum(),
        parseSalaryMonths(summary.salaryText()),
        experience.minimum(),
        experience.maximum(),
        normalizeNullable(summary.educationText()),
        requireText(detail.rawJd(), "原始 JD 不能为空"),
        normalizedJd,
        summary.publishTime(),
        Objects.requireNonNull(summary.recruiterActivity(), "招聘方活跃度不能为空"),
        summary.sourceUrl(),
        SourceStatus.ACTIVE,
        contentHash(company, title, summary.city(), normalizedJd),
        detail.rawData(),
        now,
        now,
        now,
        now);
  }

  private Range parseSalary(String value) {
    if (value == null) return Range.unknown();
    Matcher matcher = SALARY_RANGE.matcher(value);
    if (!matcher.find()) return Range.unknown();
    return new Range(
        Integer.parseInt(matcher.group(1)) * 1_000, Integer.parseInt(matcher.group(2)) * 1_000);
  }

  private Integer parseSalaryMonths(String value) {
    if (value == null) return null;
    Matcher matcher = SALARY_MONTHS.matcher(value);
    return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
  }

  private Range parseExperience(String value) {
    if (value == null) return Range.unknown();
    Matcher range = EXPERIENCE_RANGE.matcher(value);
    if (range.find()) {
      return new Range(Integer.valueOf(range.group(1)), Integer.valueOf(range.group(2)));
    }
    Matcher single = EXPERIENCE_SINGLE.matcher(value);
    if (single.find()) {
      Integer years = Integer.valueOf(single.group(1));
      return new Range(years, years);
    }
    return Range.unknown();
  }

  private String contentHash(String company, String title, String city, String normalizedJd) {
    String identity =
        company.toLowerCase(Locale.ROOT)
            + '\u001f'
            + title.toLowerCase(Locale.ROOT)
            + '\u001f'
            + Objects.requireNonNullElse(normalizeNullable(city), "UNKNOWN")
            + '\u001f'
            + normalizedJd;
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("JVM 缺少 SHA-256", exception);
    }
  }

  private static String normalizeJd(String value) {
    String raw = requireText(value, "原始 JD 不能为空");
    return raw.lines()
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .map(JobNormalizationService::normalizeInline)
        .reduce((left, right) -> left + "\n" + right)
        .orElseThrow();
  }

  private static String normalizeInline(String value) {
    return requireText(value, "文本不能为空").trim().replaceAll("\\s+", " ");
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    return value;
  }

  private record Range(Integer minimum, Integer maximum) {
    static Range unknown() {
      return new Range(null, null);
    }
  }
}
