package io.roleos.job.service;

import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate.RankingEvaluation;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.port.SemanticRankingPort;
import io.roleos.job.port.SemanticRankingPort.RankingContext;
import io.roleos.job.port.SemanticRankingPort.SemanticRankingDecision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 组合确定性规则基线与严格校验的轻量语义信号，生成 Broad Ranking。 */
public final class BroadRankingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BroadRankingService.class);

  private final Clock clock;
  private final SemanticRankingPort semanticRanking;
  private final RankingConfig config;

  public BroadRankingService(
      Clock clock, SemanticRankingPort semanticRanking, RankingConfig config) {
    this.clock = Objects.requireNonNull(clock);
    this.semanticRanking = Objects.requireNonNull(semanticRanking);
    this.config = Objects.requireNonNull(config);
  }

  /** 对 Pass/Unknown 候选排序；Reject 会在调用 Agent 前被拒绝。 */
  public RankingEvaluation rank(
      UUID candidateId, Job job, FilterResult filterResult, RankingContext context) {
    Objects.requireNonNull(candidateId);
    Objects.requireNonNull(job);
    Objects.requireNonNull(filterResult);
    Objects.requireNonNull(context);
    if (filterResult == FilterResult.REJECT) {
      throw new IllegalArgumentException("Reject 候选不得进入 Broad Ranking");
    }

    Map<String, BigDecimal> ruleSignals = ruleSignals(job);
    BigDecimal ruleScore = scoreRuleSignals(ruleSignals);
    List<String> warnings = new ArrayList<>();
    if (filterResult == FilterResult.UNKNOWN) {
      warnings.add("硬过滤包含 Unknown，排序结果需人工复核");
    }

    try {
      SemanticRankingDecision semantic =
          SemanticRankingValidator.validate(semanticRanking.rank(job, context));
      BigDecimal semanticScore = semanticScore(semantic);
      BigDecimal total =
          clamp(
              ruleScore
                  .multiply(BigDecimal.valueOf(config.ruleWeight()))
                  .add(semanticScore.multiply(BigDecimal.valueOf(config.semanticWeight()))));
      warnings.addAll(semantic.warnings());
      StrategyRecommendation recommendation =
          total.compareTo(BigDecimal.valueOf(config.targetedThreshold())) >= 0
                  && semantic.recommendation() == StrategyRecommendation.PROMOTE_TO_TARGETED
              ? StrategyRecommendation.PROMOTE_TO_TARGETED
              : semantic.recommendation() == StrategyRecommendation.SKIP
                  ? StrategyRecommendation.SKIP
                  : StrategyRecommendation.KEEP_BROAD;
      RankingEvaluation evaluation =
          evaluation(
              candidateId,
              ruleScore,
              semanticScore,
              total,
              recommendation,
              semantic.reasons(),
              semantic.importantSignals(),
              warnings,
              ruleSignals,
              semanticSignals(semantic),
              semantic.traceId());
      LOGGER
          .atInfo()
          .addKeyValue("event", "job.ranking.completed")
          .addKeyValue("candidateId", candidateId)
          .addKeyValue("status", "SUCCEEDED")
          .addKeyValue("recommendation", recommendation)
          .addKeyValue("scoringVersion", config.scoringVersion())
          .log("岗位 Broad Ranking 已完成");
      return evaluation;
    } catch (RuntimeException exception) {
      warnings.add("语义分析失败，当前仅展示确定性规则基线");
      RankingEvaluation evaluation =
          evaluation(
              candidateId,
              ruleScore,
              null,
              ruleScore,
              StrategyRecommendation.REVIEW,
              List.of("基于岗位字段完整度与来源事实生成规则基线"),
              List.of("规则基线可用"),
              warnings,
              ruleSignals,
              Map.of(),
              null);
      LOGGER
          .atWarn()
          .addKeyValue("event", "job.ranking.fallback")
          .addKeyValue("candidateId", candidateId)
          .addKeyValue("status", "DEGRADED")
          .addKeyValue("reasonCode", "SEMANTIC_RANKING_FAILED")
          .addKeyValue("scoringVersion", config.scoringVersion())
          .log("语义排序不可用，已降级为确定性规则基线");
      return evaluation;
    }
  }

  private Map<String, BigDecimal> ruleSignals(Job job) {
    int known = 0;
    if (job.city() != null) known++;
    if (job.salaryMin() != null || job.salaryMax() != null) known++;
    if (job.experienceMin() != null || job.experienceMax() != null) known++;
    if (job.educationRequirement() != null) known++;
    BigDecimal completeness = BigDecimal.valueOf(known * 100L / 4L);
    BigDecimal activity = activityPoints(job.recruiterActivity());
    Map<String, BigDecimal> signals = new LinkedHashMap<>();
    signals.put("factCompleteness", completeness);
    signals.put("recruiterActivityPoints", activity);
    return Map.copyOf(signals);
  }

  private BigDecimal scoreRuleSignals(Map<String, BigDecimal> signals) {
    BigDecimal base = signals.get("factCompleteness").multiply(new BigDecimal("0.95"));
    return clamp(base.add(signals.get("recruiterActivityPoints")));
  }

  private BigDecimal activityPoints(RecruiterActivity activity) {
    double ratio =
        switch (activity) {
          case JUST_NOW -> 1.0;
          case TODAY -> 0.8;
          case RECENT -> 0.5;
          case STALE -> 0.2;
          case UNKNOWN -> 0.0;
        };
    return BigDecimal.valueOf(config.recruiterActivityMaxPoints() * ratio)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal semanticScore(SemanticRankingDecision decision) {
    long sum =
        (long) decision.technicalStackFit().score()
            + decision.roleRelevance().score()
            + decision.careerGoalFit().score()
            + decision.jobQuality().score()
            + decision.experienceLeverage().score();
    return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
  }

  private static Map<String, BigDecimal> semanticSignals(SemanticRankingDecision decision) {
    return Map.of(
        "technicalStackFit", BigDecimal.valueOf(decision.technicalStackFit().score()),
        "roleRelevance", BigDecimal.valueOf(decision.roleRelevance().score()),
        "careerGoalFit", BigDecimal.valueOf(decision.careerGoalFit().score()),
        "jobQuality", BigDecimal.valueOf(decision.jobQuality().score()),
        "experienceLeverage", BigDecimal.valueOf(decision.experienceLeverage().score()));
  }

  private RankingEvaluation evaluation(
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
      String traceId) {
    return new RankingEvaluation(
        UUID.randomUUID(),
        candidateId,
        ruleScore,
        semanticScore,
        totalScore,
        recommendation,
        reasons,
        importantSignals,
        warnings,
        ruleSignals,
        semanticSignals,
        traceId,
        config.scoringVersion(),
        clock.instant());
  }

  private static BigDecimal clamp(BigDecimal value) {
    return value
        .max(BigDecimal.ZERO)
        .min(BigDecimal.valueOf(100))
        .setScale(2, RoundingMode.HALF_UP);
  }

  /** 可审计的 Ranking 权重与版本。 */
  public record RankingConfig(
      double ruleWeight,
      double semanticWeight,
      int recruiterActivityMaxPoints,
      int targetedThreshold,
      String scoringVersion) {
    public RankingConfig {
      if (ruleWeight < 0
          || semanticWeight < 0
          || Math.abs(ruleWeight + semanticWeight - 1.0) > 0.0001) {
        throw new IllegalArgumentException("Ranking 权重必须非负且总和为 1");
      }
      if (recruiterActivityMaxPoints < 0 || recruiterActivityMaxPoints > 5) {
        throw new IllegalArgumentException("招聘方活跃度贡献必须在 0..5 分范围内");
      }
      if (targetedThreshold < 0 || targetedThreshold > 100) {
        throw new IllegalArgumentException("Targeted 阈值必须在 0..100 范围内");
      }
      if (scoringVersion == null || scoringVersion.isBlank()) {
        throw new IllegalArgumentException("评分版本不能为空");
      }
    }
  }
}
