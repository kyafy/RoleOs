package io.roleos.browser.boss;

import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.browser.port.BrowserProvider.PageSnapshot;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.port.JobSourcePort.RawJobDetail;
import io.roleos.job.port.JobSourcePort.RawJobSummary;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 将脱敏 Accessibility Snapshot 中的 Boss 语义字段解析为来源中立 DTO。 */
public final class BossSnapshotParser {
  private final BossAccessibilityParser accessibilityParser = new BossAccessibilityParser();

  /** 解析 `JOB key=value | ...` 语义行；未知字段保持缺失，不做推断。 */
  public List<RawJobSummary> parseList(PageSnapshot snapshot) {
    detectHumanVerification(snapshot.accessibilityText());
    if (snapshot.accessibilityText().stripLeading().startsWith("{")) {
      return accessibilityParser.list(snapshot.accessibilityText());
    }
    List<RawJobSummary> jobs = new ArrayList<>();
    for (String line : snapshot.accessibilityText().lines().toList()) {
      if (!line.strip().startsWith("JOB ")) continue;
      Map<String, String> fields = fields(line.strip().substring(4));
      try {
        jobs.add(
            new RawJobSummary(
                Source.BOSS,
                required(fields, "id"),
                required(fields, "title"),
                required(fields, "company"),
                fields.get("city"),
                fields.get("salary"),
                fields.get("experience"),
                fields.get("education"),
                instant(fields.get("published")),
                activity(fields.get("activity")),
                URI.create(required(fields, "url")),
                Map.copyOf(fields)));
      } catch (RuntimeException exception) {
        throw new BrowserProviderException(
            FailureCategory.TERMINAL_FAILURE,
            "BOSS_LIST_STRUCTURE_CHANGED",
            "Boss 页面结构与预期不符",
            exception);
      }
    }
    if (jobs.isEmpty()) throw terminal("BOSS_LIST_EMPTY_OR_CHANGED");
    return List.copyOf(jobs);
  }

  public RawJobDetail parseDetail(PageSnapshot snapshot, RawJobSummary summary) {
    detectHumanVerification(snapshot.accessibilityText());
    if (snapshot.accessibilityText().stripLeading().startsWith("{")) {
      if (snapshot.accessibilityText().contains("\"cards\"")) {
        return accessibilityParser.detailFacts(snapshot.accessibilityText(), summary);
      }
      String jd = accessibilityParser.detail(snapshot.accessibilityText());
      return new RawJobDetail(
          summary,
          jd,
          Map.of("jd", jd, "listFacts", summary.rawData()),
          snapshot.capturedAt(),
          io.roleos.job.domain.JobTypes.BrowserProviderType.KIMI_WEBBRIDGE);
    }
    String prefix = "DETAIL ";
    String line =
        snapshot
            .accessibilityText()
            .lines()
            .filter(value -> value.strip().startsWith(prefix))
            .findFirst()
            .orElseThrow(() -> terminal("BOSS_DETAIL_STRUCTURE_CHANGED"));
    Map<String, String> fields = fields(line.strip().substring(prefix.length()));
    String jd = required(fields, "jd");
    return new RawJobDetail(
        summary,
        jd,
        Map.copyOf(fields),
        snapshot.capturedAt(),
        summary.rawData().containsKey("provider")
            ? io.roleos.job.domain.JobTypes.BrowserProviderType.valueOf(
                summary.rawData().get("provider").toString())
            : io.roleos.job.domain.JobTypes.BrowserProviderType.AUTO);
  }

  private static Map<String, String> fields(String line) {
    Map<String, String> result = new LinkedHashMap<>();
    for (String part : line.split("\\s*\\|\\s*")) {
      int separator = part.indexOf('=');
      if (separator > 0)
        result.put(part.substring(0, separator).strip(), part.substring(separator + 1).strip());
    }
    return result;
  }

  private static String required(Map<String, String> fields, String key) {
    String value = fields.get(key);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("missing " + key);
    return value;
  }

  private static Instant instant(String value) {
    return value == null || value.isBlank() ? null : Instant.parse(value);
  }

  private static RecruiterActivity activity(String value) {
    if (value == null || value.isBlank()) return RecruiterActivity.UNKNOWN;
    try {
      return RecruiterActivity.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return RecruiterActivity.UNKNOWN;
    }
  }

  private static void detectHumanVerification(String text) {
    String lower = text.toLowerCase(Locale.ROOT);
    if (lower.contains("captcha")
        || text.contains("验证码")
        || text.contains("安全验证")
        || text.contains("风险提示")) {
      throw new BrowserProviderException(
          FailureCategory.PAUSE_FOR_HUMAN, "HUMAN_VERIFICATION_REQUIRED", "Boss 页面要求人工验证");
    }
  }

  private static BrowserProviderException terminal(String code) {
    return new BrowserProviderException(FailureCategory.TERMINAL_FAILURE, code, "Boss 页面结构无法安全解析");
  }
}
