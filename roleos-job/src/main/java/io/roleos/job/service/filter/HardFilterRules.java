package io.roleos.job.service.filter;

import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate.RuleResult;
import io.roleos.job.domain.JobTypes.FilterResult;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Job Intelligence 的确定性硬筛规则。 */
public final class HardFilterRules {

  private HardFilterRules() {}

  /** 按目标城市判断；Remote 与包含目标城市的多城市岗位通过。 */
  public static RuleResult city(Job job, String targetCity) {
    if (job.city() == null || job.city().isBlank()) {
      return result(FilterResult.UNKNOWN, "CITY_UNKNOWN", "岗位未提供城市", Map.of());
    }
    String actual = normalize(job.city());
    String expected = normalize(targetCity);
    boolean matches =
        actual.contains("remote") || (!expected.isBlank() && actual.contains(expected));
    return result(
        matches ? FilterResult.PASS : FilterResult.REJECT,
        matches ? "CITY_MATCH" : "CITY_MISMATCH",
        matches ? "岗位城市符合目标城市或支持远程" : "岗位城市不符合目标城市",
        Map.of("jobCity", job.city(), "targetCity", targetCity));
  }

  /** 按岗位薪资上界与用户最低可接受月薪判断。 */
  public static RuleResult salary(Job job, Integer acceptedMinimum) {
    if (acceptedMinimum == null) {
      return result(FilterResult.UNKNOWN, "SALARY_PREFERENCE_UNKNOWN", "用户未提供最低薪资偏好", Map.of());
    }
    if (job.salaryMin() == null && job.salaryMax() == null) {
      return result(FilterResult.UNKNOWN, "SALARY_UNKNOWN", "岗位未提供薪资范围", Map.of());
    }
    boolean rejected = job.salaryMax() != null && job.salaryMax() < acceptedMinimum;
    return result(
        rejected ? FilterResult.REJECT : FilterResult.PASS,
        rejected ? "SALARY_BELOW_MINIMUM" : "SALARY_OVERLAPS_MINIMUM",
        rejected ? "岗位最高月薪低于最低期望" : "岗位薪资范围与最低期望存在交集",
        nullableFacts(
            "salaryMin",
            job.salaryMin(),
            "salaryMax",
            job.salaryMax(),
            "acceptedMinimum",
            acceptedMinimum));
  }

  /** 按岗位最低经验要求判断；缺失最低值时不推断为通过。 */
  public static RuleResult experience(Job job, Integer userExperienceYears) {
    if (userExperienceYears == null) {
      return result(FilterResult.UNKNOWN, "PROFILE_EXPERIENCE_UNKNOWN", "用户经验年限未知", Map.of());
    }
    if (job.experienceMin() == null) {
      return result(
          FilterResult.UNKNOWN,
          "EXPERIENCE_MINIMUM_UNKNOWN",
          "岗位未提供最低经验要求",
          nullableFacts("experienceMax", job.experienceMax()));
    }
    boolean accepted = job.experienceMin() <= userExperienceYears;
    return result(
        accepted ? FilterResult.PASS : FilterResult.REJECT,
        accepted ? "EXPERIENCE_WITHIN_RANGE" : "EXPERIENCE_ABOVE_PROFILE",
        accepted ? "用户经验满足岗位最低要求" : "岗位最低经验要求超出用户当前经验",
        nullableFacts(
            "experienceMin",
            job.experienceMin(),
            "experienceMax",
            job.experienceMax(),
            "userExperienceYears",
            userExperienceYears));
  }

  /** 使用规范化词元重叠判断目标岗位，不调用模型，确保结果可复现。 */
  public static RuleResult targetRole(Job job, String targetRole) {
    String actual = normalize(job.normalizedTitle());
    String expected = normalize(targetRole);
    if (actual.isBlank() || expected.isBlank()) {
      return result(FilterResult.UNKNOWN, "ROLE_UNKNOWN", "岗位名称或目标岗位缺失", Map.of());
    }
    Set<String> expectedTokens = tokens(expected);
    Set<String> actualTokens = tokens(actual);
    boolean matches =
        actual.contains(expected)
            || expected.contains(actual)
            || actualTokens.stream().anyMatch(expectedTokens::contains);
    return result(
        matches ? FilterResult.PASS : FilterResult.REJECT,
        matches ? "ROLE_MATCH" : "ROLE_MISMATCH",
        matches ? "岗位名称与目标岗位匹配" : "岗位名称与目标岗位不匹配",
        Map.of("jobTitle", job.title(), "targetRole", targetRole));
  }

  private static Set<String> tokens(String value) {
    return Arrays.stream(value.split("[^\\p{L}\\p{N}]+"))
        .filter(token -> token.length() >= 2)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static RuleResult result(
      FilterResult result, String code, String explanation, Map<String, Object> facts) {
    return new RuleResult(result, code, explanation, facts);
  }

  private static Map<String, Object> nullableFacts(Object... entries) {
    var facts = new java.util.LinkedHashMap<String, Object>();
    for (int index = 0; index < entries.length; index += 2) {
      if (entries[index + 1] != null) {
        facts.put((String) entries[index], entries[index + 1]);
      }
    }
    return facts;
  }
}
