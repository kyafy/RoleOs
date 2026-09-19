package io.roleos.application.career.profile;

import io.roleos.domain.career.CommandId;
import java.util.Objects;

/** 更新当前用户职业档案的幂等命令。 */
public record UpsertCareerProfileCommand(
    CommandId commandId, String displayName, String careerDirection, String targetRolePreference) {
  public UpsertCareerProfileCommand {
    Objects.requireNonNull(commandId, "commandId 不能为空");
    requireText(displayName, "displayName 不能为空");
    requireText(careerDirection, "careerDirection 不能为空");
    requireText(targetRolePreference, "targetRolePreference 不能为空");
  }

  private static void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
