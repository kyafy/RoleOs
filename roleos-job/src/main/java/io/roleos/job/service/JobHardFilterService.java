package io.roleos.job.service;

import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate.FilterEvaluation;
import io.roleos.job.domain.JobCandidate.RuleResult;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.service.filter.HardFilterRules;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 执行城市、薪资、经验和目标岗位四项确定性硬筛，并保留逐项解释。 */
public final class JobHardFilterService {

  private final Clock clock;
  private final String ruleVersion;

  public JobHardFilterService(Clock clock, String ruleVersion) {
    this.clock = Objects.requireNonNull(clock);
    if (ruleVersion == null || ruleVersion.isBlank()) {
      throw new IllegalArgumentException("硬筛规则版本不能为空");
    }
    this.ruleVersion = ruleVersion;
  }

  /**
   * 对候选岗位执行硬筛。任一规则拒绝即拒绝；无拒绝但存在未知则为未知；否则通过。
   *
   * @param candidateId 候选关系标识
   * @param job Canonical Job
   * @param criteria 用户当前筛选条件
   * @return 包含每项规则证据的过滤结果
   */
  public FilterEvaluation evaluate(UUID candidateId, Job job, FilterCriteria criteria) {
    Objects.requireNonNull(candidateId);
    Objects.requireNonNull(job);
    Objects.requireNonNull(criteria);
    RuleResult city = HardFilterRules.city(job, criteria.targetCity());
    RuleResult salary = HardFilterRules.salary(job, criteria.acceptedSalaryMinimum());
    RuleResult experience = HardFilterRules.experience(job, criteria.userExperienceYears());
    RuleResult targetRole = HardFilterRules.targetRole(job, criteria.targetRole());
    FilterResult overall = aggregate(List.of(city, salary, experience, targetRole));
    return new FilterEvaluation(
        UUID.randomUUID(),
        candidateId,
        overall,
        city,
        salary,
        experience,
        targetRole,
        ruleVersion,
        clock.instant());
  }

  private static FilterResult aggregate(List<RuleResult> rules) {
    if (rules.stream().anyMatch(rule -> rule.result() == FilterResult.REJECT)) {
      return FilterResult.REJECT;
    }
    if (rules.stream().anyMatch(rule -> rule.result() == FilterResult.UNKNOWN)) {
      return FilterResult.UNKNOWN;
    }
    return FilterResult.PASS;
  }

  /** 用户确认的硬筛输入；数值均使用标准化后的整数单位。 */
  public record FilterCriteria(
      String targetCity,
      Integer acceptedSalaryMinimum,
      Integer userExperienceYears,
      String targetRole) {
    public FilterCriteria {
      if (targetCity == null || targetCity.isBlank()) {
        throw new IllegalArgumentException("目标城市不能为空");
      }
      if (acceptedSalaryMinimum != null && acceptedSalaryMinimum < 0) {
        throw new IllegalArgumentException("最低可接受月薪不能为负数");
      }
      if (userExperienceYears != null && (userExperienceYears < 0 || userExperienceYears > 60)) {
        throw new IllegalArgumentException("用户经验年限必须在 0..60 范围内");
      }
      if (targetRole == null || targetRole.isBlank()) {
        throw new IllegalArgumentException("目标岗位不能为空");
      }
    }
  }
}
