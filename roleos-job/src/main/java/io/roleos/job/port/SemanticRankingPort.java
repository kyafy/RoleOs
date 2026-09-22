package io.roleos.job.port;

import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import java.util.List;

/** 轻量语义判断端口；返回值仍需由 Job 模块进行 Schema 与分值校验。 */
@FunctionalInterface
public interface SemanticRankingPort {

  SemanticRankingDecision rank(Job job, RankingContext context);

  record RankingContext(String targetRole, List<String> skills, String careerGoal) {
    public RankingContext {
      skills = skills == null ? List.of() : List.copyOf(skills);
    }
  }

  record Signal(int score, String explanation) {}

  @SuppressWarnings("EI_EXPOSE_REP") // Compact constructor converts lists to immutable copies.
  record SemanticRankingDecision(
      Signal technicalStackFit,
      Signal roleRelevance,
      Signal careerGoalFit,
      Signal jobQuality,
      Signal experienceLeverage,
      StrategyRecommendation recommendation,
      List<String> reasons,
      List<String> importantSignals,
      List<String> warnings,
      String traceId) {
    public SemanticRankingDecision {
      reasons = reasons == null ? List.of() : List.copyOf(reasons);
      importantSignals = importantSignals == null ? List.of() : List.copyOf(importantSignals);
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
  }
}
