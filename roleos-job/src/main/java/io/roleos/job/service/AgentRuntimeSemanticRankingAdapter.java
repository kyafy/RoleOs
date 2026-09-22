package io.roleos.job.service;

import io.roleos.domain.agent.AgentRequest;
import io.roleos.domain.agent.AgentRuntimePort;
import io.roleos.job.domain.Job;
import io.roleos.job.port.SemanticRankingPort;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 通过通用 AgentRuntimePort 获取语义信号，不赋予 Agent 状态写入能力。 */
public final class AgentRuntimeSemanticRankingAdapter implements SemanticRankingPort {

  private static final int MAX_JD_CONTEXT_LENGTH = 2_000;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AgentRuntimeSemanticRankingAdapter.class);

  private final AgentRuntimePort runtime;
  private final SemanticRankingValidator validator;

  public AgentRuntimeSemanticRankingAdapter(
      AgentRuntimePort runtime, SemanticRankingValidator validator) {
    this.runtime = Objects.requireNonNull(runtime);
    this.validator = Objects.requireNonNull(validator);
  }

  @Override
  public SemanticRankingDecision rank(Job job, RankingContext context) {
    String traceId = UUID.randomUUID().toString();
    var requestContext = new LinkedHashMap<String, String>();
    requestContext.put("jobTitle", job.normalizedTitle());
    requestContext.put("jobDescription", truncate(job.normalizedJd()));
    requestContext.put("targetRole", context.targetRole());
    requestContext.put("skills", String.join(",", context.skills()));
    requestContext.put("careerGoal", context.careerGoal());
    var decision =
        runtime.decide(
            new AgentRequest(
                traceId,
                "仅依据上下文返回 job-semantic-ranking-v1 严格 JSON；不得执行工具、修改状态或补造事实。",
                requestContext));
    String output = decision.attributes().get("structuredOutput");
    if (output == null) {
      throw new IllegalArgumentException("Agent 未返回 structuredOutput");
    }
    SemanticRankingDecision parsed = validator.parse(output, traceId);
    LOGGER
        .atInfo()
        .addKeyValue("event", "agent.semantic_ranking.completed")
        .addKeyValue("jobId", job.id())
        .addKeyValue("traceId", traceId)
        .addKeyValue("provider", "agent_runtime")
        .addKeyValue("status", "SUCCEEDED")
        .log("Agent 语义排序结构化结果已通过校验");
    return parsed;
  }

  private static String truncate(String value) {
    return value.length() <= MAX_JD_CONTEXT_LENGTH
        ? value
        : value.substring(0, MAX_JD_CONTEXT_LENGTH);
  }
}
