package io.roleos.job.domain;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 用户与 Canonical Job 的候选关系，拥有过滤、排序与用户策略。 */
public record JobCandidate(
    UUID id,
    UserId userId,
    UUID jobId,
    FilterResult filterResult,
    BigDecimal rankingScore,
    JobStrategy strategy,
    StrategyRecommendation strategyRecommendation,
    SemanticAnalysisStatus semanticAnalysisStatus,
    String decisionReason,
    Map<String, Object> metadata,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  public JobCandidate {
    Objects.requireNonNull(id, "候选标识不能为空");
    Objects.requireNonNull(userId, "用户不能为空");
    Objects.requireNonNull(jobId, "岗位不能为空");
    Objects.requireNonNull(filterResult, "过滤结果不能为空");
    validateScore(rankingScore, "岗位排序分");
    Objects.requireNonNull(strategy, "岗位策略不能为空");
    Objects.requireNonNull(strategyRecommendation, "策略建议不能为空");
    Objects.requireNonNull(semanticAnalysisStatus, "语义分析状态不能为空");
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    if (version < 0) throw new IllegalArgumentException("版本不能为负数");
    Objects.requireNonNull(createdAt, "创建时间不能为空");
    Objects.requireNonNull(updatedAt, "更新时间不能为空");
    if (filterResult == FilterResult.REJECT && rankingScore != null) {
      throw new IllegalArgumentException("被拒绝的候选不能拥有排序分");
    }
  }

  /** 确定性硬过滤的当前结果，四项规则必须全部存在。 */
  public record FilterEvaluation(
      UUID id,
      UUID candidateId,
      FilterResult overallResult,
      RuleResult city,
      RuleResult salary,
      RuleResult experience,
      RuleResult targetRole,
      String ruleVersion,
      Instant evaluatedAt) {
    public FilterEvaluation {
      Objects.requireNonNull(id);
      Objects.requireNonNull(candidateId);
      Objects.requireNonNull(overallResult);
      Objects.requireNonNull(city);
      Objects.requireNonNull(salary);
      Objects.requireNonNull(experience);
      Objects.requireNonNull(targetRole);
      requireText(ruleVersion, "规则版本不能为空");
      Objects.requireNonNull(evaluatedAt);
    }
  }

  /** 单项过滤事实、原因码和面向用户的解释。 */
  public record RuleResult(
      FilterResult result, String reasonCode, String explanation, Map<String, Object> factsUsed) {
    public RuleResult {
      Objects.requireNonNull(result);
      requireText(reasonCode, "过滤原因码不能为空");
      requireText(explanation, "过滤解释不能为空");
      factsUsed = factsUsed == null ? Map.of() : Map.copyOf(factsUsed);
    }
  }

  /** 可解释的规则与语义混合排序结果。 */
  @SuppressWarnings(
      "EI_EXPOSE_REP") // Compact constructor converts every collection to immutable copies.
  public record RankingEvaluation(
      UUID id,
      UUID candidateId,
      BigDecimal ruleScore,
      BigDecimal semanticScore,
      BigDecimal totalScore,
      StrategyRecommendation recommendation,
      List<String> reasons,
      List<String> importantSignals,
      List<String> warnings,
      Map<String, BigDecimal> ruleSignals,
      Map<String, BigDecimal> semanticSignals,
      String agentTraceId,
      String scoringVersion,
      Instant evaluatedAt) {
    public RankingEvaluation {
      Objects.requireNonNull(id);
      Objects.requireNonNull(candidateId);
      validateScore(ruleScore, "规则分");
      validateScore(semanticScore, "语义分");
      validateScore(totalScore, "总分");
      Objects.requireNonNull(recommendation);
      reasons = copyStrings(reasons);
      importantSignals = copyStrings(importantSignals);
      warnings = copyStrings(warnings);
      ruleSignals = ruleSignals == null ? Map.of() : Map.copyOf(ruleSignals);
      semanticSignals = semanticSignals == null ? Map.of() : Map.copyOf(semanticSignals);
      requireText(scoringVersion, "评分版本不能为空");
      Objects.requireNonNull(evaluatedAt);
    }

    @Override
    public List<String> reasons() {
      return List.copyOf(reasons);
    }

    @Override
    public List<String> importantSignals() {
      return List.copyOf(importantSignals);
    }

    @Override
    public List<String> warnings() {
      return List.copyOf(warnings);
    }
  }

  private static List<String> copyStrings(List<String> values) {
    return values == null ? List.of() : List.copyOf(values);
  }

  private static void validateScore(BigDecimal score, String label) {
    if (score != null
        && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0)) {
      throw new IllegalArgumentException(label + "必须在 0..100 范围内");
    }
  }

  private static void requireText(String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
  }
}
