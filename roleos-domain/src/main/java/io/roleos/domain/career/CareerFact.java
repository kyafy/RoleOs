package io.roleos.domain.career;

import java.util.Objects;

/** 已进入 Career Database 的职业事实。 */
public interface CareerFact {
  UserId userId();

  ProvenanceType provenanceType();

  ConfirmationStatus confirmationStatus();

  default void requireEligibleForFactStore() {
    Objects.requireNonNull(userId(), "职业事实必须归属用户");
    if (provenanceType() == ProvenanceType.AGENT_INFERENCE
        && confirmationStatus() == ConfirmationStatus.CONFIRMED) {
      throw new IllegalStateException("Agent 推断未经用户确认或证据支持，不得成为已确认职业事实");
    }
    if (confirmationStatus() != ConfirmationStatus.CONFIRMED) {
      throw new IllegalStateException("只有已确认信息才能作为职业事实保存");
    }
  }
}
