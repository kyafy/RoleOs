package io.roleos.domain.agent;

import java.util.Map;
import java.util.Objects;

/** 面向 Runtime 的最小、可审计请求载体。 */
public record AgentRequest(String traceId, String instruction, Map<String, String> context) {

  public AgentRequest {
    Objects.requireNonNull(traceId, "traceId 不能为空");
    Objects.requireNonNull(instruction, "instruction 不能为空");
    context = context == null ? Map.of() : Map.copyOf(context);
  }
}
