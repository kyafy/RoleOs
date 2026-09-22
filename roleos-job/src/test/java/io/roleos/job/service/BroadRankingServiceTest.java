package io.roleos.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.port.SemanticRankingPort;
import io.roleos.job.port.SemanticRankingPort.RankingContext;
import io.roleos.job.port.SemanticRankingPort.SemanticRankingDecision;
import io.roleos.job.port.SemanticRankingPort.Signal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Broad Ranking 必须可解释、可降级，且活跃度始终是弱信号。 */
class BroadRankingServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-19T07:00:00Z");

  @Test
  void combinesRuleAndSemanticSignalsWithinZeroToOneHundred() {
    var result = service(port(90)).rank(UUID.randomUUID(), job(), FilterResult.PASS, context());

    assertThat(result.totalScore())
        .isBetween(new java.math.BigDecimal("0"), new java.math.BigDecimal("100"));
    assertThat(result.semanticScore()).isNotNull();
    assertThat(result.reasons()).isNotEmpty();
    assertThat(result.importantSignals()).isNotEmpty();
    assertThat(result.recommendation()).isEqualTo(StrategyRecommendation.PROMOTE_TO_TARGETED);
  }

  @Test
  void rejectNeverEntersRanking() {
    assertThatThrownBy(
            () -> service(port(90)).rank(UUID.randomUUID(), job(), FilterResult.REJECT, context()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Reject");
  }

  @Test
  void unknownRemainsVisibleAsWarning() {
    var result = service(port(80)).rank(UUID.randomUUID(), job(), FilterResult.UNKNOWN, context());

    assertThat(result.warnings()).anyMatch(value -> value.contains("Unknown"));
  }

  @Test
  void semanticFailureKeepsRuleBaselineAndMarksWarning() {
    SemanticRankingPort failed =
        (job, context) -> {
          throw new IllegalStateException("down");
        };
    var result = service(failed).rank(UUID.randomUUID(), job(), FilterResult.PASS, context());

    assertThat(result.semanticScore()).isNull();
    assertThat(result.ruleScore()).isPositive();
    assertThat(result.warnings()).anyMatch(value -> value.contains("语义分析失败"));
    assertThat(result.recommendation()).isEqualTo(StrategyRecommendation.REVIEW);
  }

  @Test
  void recruiterActivityContributionNeverExceedsConfiguredFivePoints() {
    var active =
        service(port(70))
            .rank(UUID.randomUUID(), job(RecruiterActivity.JUST_NOW), FilterResult.PASS, context());
    var stale =
        service(port(70))
            .rank(UUID.randomUUID(), job(RecruiterActivity.STALE), FilterResult.PASS, context());

    assertThat(active.ruleScore().subtract(stale.ruleScore()).abs())
        .isLessThanOrEqualTo(new java.math.BigDecimal("5.00"));
  }

  @Test
  void strongCoreFitOutranksActivityOnlySignal() {
    var strong =
        service(port(95))
            .rank(UUID.randomUUID(), job(RecruiterActivity.STALE), FilterResult.PASS, context());
    var weak =
        service(port(20))
            .rank(UUID.randomUUID(), job(RecruiterActivity.JUST_NOW), FilterResult.PASS, context());

    assertThat(strong.totalScore()).isGreaterThan(weak.totalScore());
  }

  private BroadRankingService service(SemanticRankingPort port) {
    return new BroadRankingService(
        Clock.fixed(NOW, ZoneOffset.UTC),
        port,
        new BroadRankingService.RankingConfig(0.45, 0.55, 5, 80, "ranking-v1"));
  }

  private RankingContext context() {
    return new RankingContext("AI Agent Engineer", List.of("Java", "MCP"), "AI Agent 平台工程师");
  }

  private SemanticRankingPort port(int score) {
    return (job, context) ->
        new SemanticRankingDecision(
            new Signal(score, "技术匹配"),
            new Signal(score, "方向匹配"),
            new Signal(score, "目标匹配"),
            new Signal(score, "岗位质量"),
            new Signal(score, "经验复用"),
            score >= 80
                ? StrategyRecommendation.PROMOTE_TO_TARGETED
                : StrategyRecommendation.KEEP_BROAD,
            List.of("结构化语义判断"),
            List.of("核心相关性"),
            List.of(),
            "trace-fixture");
  }

  static Job job() {
    return job(RecruiterActivity.TODAY);
  }

  private static Job job(RecruiterActivity activity) {
    return new Job(
        UUID.randomUUID(),
        UserId.random(),
        Source.BOSS,
        UUID.randomUUID().toString(),
        "AI Agent 工程师",
        "ai agent 工程师",
        "RoleOS",
        "roleos",
        "上海",
        25_000,
        35_000,
        14,
        3,
        5,
        "本科",
        "负责 Java、MCP 与 Agent 平台",
        "负责 Java、MCP 与 Agent 平台",
        NOW,
        activity,
        URI.create("https://www.zhipin.com/job_detail/fixture.html"),
        SourceStatus.ACTIVE,
        "f".repeat(64),
        Map.of(),
        NOW,
        NOW,
        NOW,
        NOW);
  }
}
