package io.roleos.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.agent.AgentDecision;
import io.roleos.domain.agent.AgentRuntimePort;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.port.SemanticRankingPort.RankingContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ranking Agent 输出必须严格符合白名单结构，非法结果不得进入领域层。 */
class SemanticRankingContractTest {

  private final SemanticRankingValidator validator =
      new SemanticRankingValidator(new ObjectMapper());

  @Test
  void acceptsCompleteSchemaAndMapsTraceId() {
    var decision = validator.parse(validJson(), "trace-1");

    assertThat(decision.technicalStackFit().score()).isEqualTo(90);
    assertThat(decision.recommendation()).isEqualTo(StrategyRecommendation.PROMOTE_TO_TARGETED);
    assertThat(decision.traceId()).isEqualTo("trace-1");
  }

  @Test
  void rejectsMissingFieldOutOfRangeAndAdditionalField() {
    assertThatThrownBy(() -> validator.parse(validJson().replace("\"warnings\":[]", ""), "trace"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> validator.parse(validJson().replace("\"score\":90", "\"score\":101"), "trace"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                validator.parse(validJson().replaceFirst("\\{", "{\"unexpected\":true,"), "trace"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void adapterPropagatesRuntimeFailureWithoutInventingDecision() {
    AgentRuntimePort failed =
        request -> {
          throw new IllegalStateException("runtime unavailable");
        };
    var adapter = new AgentRuntimeSemanticRankingAdapter(failed, validator);

    assertThatThrownBy(
            () ->
                adapter.rank(
                    BroadRankingServiceTest.job(),
                    new RankingContext("AI Agent Engineer", List.of("Java"), "AI Agent")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("runtime unavailable");
  }

  @Test
  void adapterReadsOnlyStructuredOutputAttribute() {
    AgentRuntimePort runtime =
        request -> new AgentDecision("ignored summary", Map.of("structuredOutput", validJson()));
    var adapter = new AgentRuntimeSemanticRankingAdapter(runtime, validator);

    var result =
        adapter.rank(
            BroadRankingServiceTest.job(),
            new RankingContext("AI Agent Engineer", List.of("Java"), "AI Agent"));

    assertThat(result.reasons()).containsExactly("技术栈匹配");
    assertThat(result.traceId()).isNotBlank();
  }

  static String validJson() {
    return """
        {
          "technicalStackFit":{"score":90,"explanation":"Java 与 Agent 技术栈匹配"},
          "roleRelevance":{"score":85,"explanation":"岗位方向匹配"},
          "careerGoalFit":{"score":80,"explanation":"符合职业目标"},
          "jobQuality":{"score":75,"explanation":"职责清晰"},
          "experienceLeverage":{"score":70,"explanation":"经验可复用"},
          "recommendation":"PROMOTE_TO_TARGETED",
          "reasons":["技术栈匹配"],
          "importantSignals":["Java","Agent"],
          "warnings":[]
        }
        """;
  }
}
