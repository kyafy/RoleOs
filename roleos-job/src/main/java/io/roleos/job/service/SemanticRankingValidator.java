package io.roleos.job.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.port.SemanticRankingPort.SemanticRankingDecision;
import io.roleos.job.port.SemanticRankingPort.Signal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 对 Ranking Agent JSON 执行字段白名单、类型、范围和文本长度校验。 */
public final class SemanticRankingValidator {

  private static final Set<String> ROOT_FIELDS =
      Set.of(
          "technicalStackFit",
          "roleRelevance",
          "careerGoalFit",
          "jobQuality",
          "experienceLeverage",
          "recommendation",
          "reasons",
          "importantSignals",
          "warnings");
  private static final Set<String> SIGNAL_FIELDS = Set.of("score", "explanation");

  private final ObjectMapper objectMapper;

  public SemanticRankingValidator(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper).copy();
  }

  /** 将严格 JSON 转为领域判断；未知字段、缺失字段或非法值都会被拒绝。 */
  public SemanticRankingDecision parse(String json, String traceId) {
    try {
      JsonNode root = objectMapper.readTree(json);
      requireExactFields(root, ROOT_FIELDS, "根对象");
      var decision =
          new SemanticRankingDecision(
              signal(root, "technicalStackFit"),
              signal(root, "roleRelevance"),
              signal(root, "careerGoalFit"),
              signal(root, "jobQuality"),
              signal(root, "experienceLeverage"),
              recommendation(root),
              textList(root, "reasons"),
              textList(root, "importantSignals"),
              textList(root, "warnings"),
              requireText(traceId, 200, "traceId"));
      return validate(decision);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Ranking Agent 输出不是合法 JSON", exception);
    }
  }

  /** 校验由其他 SemanticRankingPort 实现返回的强类型结果。 */
  public static SemanticRankingDecision validate(SemanticRankingDecision decision) {
    Objects.requireNonNull(decision, "语义判断不能为空");
    validateSignal(decision.technicalStackFit(), "technicalStackFit");
    validateSignal(decision.roleRelevance(), "roleRelevance");
    validateSignal(decision.careerGoalFit(), "careerGoalFit");
    validateSignal(decision.jobQuality(), "jobQuality");
    validateSignal(decision.experienceLeverage(), "experienceLeverage");
    Objects.requireNonNull(decision.recommendation(), "recommendation 不能为空");
    validateTextList(decision.reasons(), "reasons");
    validateTextList(decision.importantSignals(), "importantSignals");
    validateTextList(decision.warnings(), "warnings");
    requireText(decision.traceId(), 200, "traceId");
    return decision;
  }

  private Signal signal(JsonNode root, String field) {
    JsonNode node = root.get(field);
    requireExactFields(node, SIGNAL_FIELDS, field);
    JsonNode scoreNode = node.get("score");
    if (!scoreNode.isIntegralNumber()) {
      throw new IllegalArgumentException(field + ".score 必须是整数");
    }
    JsonNode explanationNode = node.get("explanation");
    if (!explanationNode.isTextual()) {
      throw new IllegalArgumentException(field + ".explanation 必须是字符串");
    }
    return new Signal(
        scoreNode.intValue(),
        requireText(explanationNode.textValue(), 500, field + ".explanation"));
  }

  private static StrategyRecommendation recommendation(JsonNode root) {
    JsonNode node = root.get("recommendation");
    if (!node.isTextual()) {
      throw new IllegalArgumentException("recommendation 必须是字符串");
    }
    try {
      return StrategyRecommendation.valueOf(node.textValue());
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("recommendation 不在允许范围内", exception);
    }
  }

  private static List<String> textList(JsonNode root, String field) {
    JsonNode node = root.get(field);
    if (!node.isArray() || node.size() > 10) {
      throw new IllegalArgumentException(field + " 必须是最多 10 项的数组");
    }
    List<String> values = new ArrayList<>();
    node.forEach(
        value -> {
          if (!value.isTextual()) {
            throw new IllegalArgumentException(field + " 只能包含字符串");
          }
          values.add(requireText(value.textValue(), 300, field));
        });
    return List.copyOf(values);
  }

  private static void requireExactFields(JsonNode node, Set<String> expected, String label) {
    if (node == null || !node.isObject()) {
      throw new IllegalArgumentException(label + " 必须是对象");
    }
    Set<String> actual = new HashSet<>();
    node.fieldNames().forEachRemaining(actual::add);
    if (!actual.equals(expected)) {
      throw new IllegalArgumentException(label + " 字段不符合结构契约");
    }
  }

  private static void validateSignal(Signal signal, String label) {
    Objects.requireNonNull(signal, label + " 不能为空");
    if (signal.score() < 0 || signal.score() > 100) {
      throw new IllegalArgumentException(label + ".score 必须在 0..100 范围内");
    }
    requireText(signal.explanation(), 500, label + ".explanation");
  }

  private static void validateTextList(List<String> values, String label) {
    if (values == null || values.size() > 10) {
      throw new IllegalArgumentException(label + " 最多允许 10 项");
    }
    values.forEach(value -> requireText(value, 300, label));
  }

  private static String requireText(String value, int maximum, String label) {
    if (value == null || value.isBlank() || value.length() > maximum) {
      throw new IllegalArgumentException(label + " 必须是 1.." + maximum + " 字符的文本");
    }
    return value;
  }
}
