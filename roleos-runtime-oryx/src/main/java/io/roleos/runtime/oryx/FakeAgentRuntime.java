package io.roleos.runtime.oryx;

import io.roleos.domain.agent.AgentDecision;
import io.roleos.domain.agent.AgentRequest;
import io.roleos.domain.agent.AgentRuntimePort;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 本地开发与契约测试使用的确定性 Runtime。
 *
 * <p>它不调用 OryxOS、OpenAI 或任何模型服务，也不会代表真实职业判断。
 */
@Component
public final class FakeAgentRuntime implements AgentRuntimePort {

  private static final Logger LOGGER = LoggerFactory.getLogger(FakeAgentRuntime.class);

  @Override
  public AgentDecision decide(AgentRequest request) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Fake Agent Runtime 已处理请求，traceId={}", request.traceId());
    }
    return new AgentDecision(
        "Fake Runtime 已接受结构化判断请求；未执行模型推理。",
        Map.of("runtime", "fake", "traceId", request.traceId()));
  }
}
