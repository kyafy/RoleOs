package io.roleos.domain.career;

/** 职业信息的来源类型，禁止将推断伪装成用户事实。 */
public enum ProvenanceType {
  USER_INPUT,
  USER_CONFIRMED,
  AGENT_INFERENCE,
  DESIGNED_SCENARIO,
  PROJECT_UPGRADE,
  SOURCE_CODE,
  EVIDENCE
}
