package io.roleos.domain.agent;

/** 职业判断类 Agent Runtime 的领域端口，隔离具体 SDK 与工具。 */
@FunctionalInterface
public interface AgentRuntimePort {

  /** 执行一次不带副作用的判断请求。 */
  AgentDecision decide(AgentRequest request);
}
