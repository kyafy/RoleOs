package io.roleos.domain.agent;

import java.util.Map;
import java.util.Objects;

/** Agent 的结构化判断结果；它不是工作流状态转换命令。 */
public record AgentDecision(String summary, Map<String, String> attributes) {

  public AgentDecision {
    Objects.requireNonNull(summary, "summary 不能为空");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
