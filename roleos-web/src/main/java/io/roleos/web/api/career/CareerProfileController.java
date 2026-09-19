package io.roleos.web.api.career;

import io.roleos.application.career.profile.CareerProfileApplicationService;
import io.roleos.application.career.profile.UpsertCareerProfileCommand;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.profile.CareerProfile;
import io.roleos.web.api.ApiResponse;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.error.RoleOsException;
import io.roleos.web.security.CurrentUserResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户职业档案 API。 */
@RestController
@RequestMapping("/api/v1/career-profile")
@SuppressWarnings("PMD.PreserveStackTrace")
public class CareerProfileController {
  private final CareerProfileApplicationService service;
  private final CurrentUserResolver currentUser;

  public CareerProfileController(
      CareerProfileApplicationService service, CurrentUserResolver currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  ApiResponse<ProfileResponse> get() {
    try {
      return ApiResponse.success(
          ProfileResponse.from(service.get(currentUser.requireCurrentUser())), traceId());
    } catch (IllegalArgumentException exception) {
      throw new RoleOsException(
          ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, exception.getMessage());
    }
  }

  @PutMapping
  ApiResponse<ProfileResponse> put(@Valid @RequestBody ProfileRequest request) {
    CareerProfile profile =
        service.upsert(
            currentUser.requireCurrentUser(),
            new UpsertCareerProfileCommand(
                new CommandId(request.commandId()),
                request.displayName(),
                request.careerDirection(),
                request.targetRolePreference()));
    return ApiResponse.success(ProfileResponse.from(profile), traceId());
  }

  private String traceId() {
    return java.util.Objects.requireNonNullElse(MDC.get("traceId"), "unknown");
  }

  record ProfileRequest(
      @NotNull UUID commandId,
      @NotBlank String displayName,
      @NotBlank String careerDirection,
      @NotBlank String targetRolePreference) {}

  record ProfileResponse(
      UUID id, String displayName, String careerDirection, String targetRolePreference) {
    static ProfileResponse from(CareerProfile profile) {
      return new ProfileResponse(
          profile.id(),
          profile.displayName(),
          profile.careerDirection(),
          profile.targetRolePreference());
    }
  }
}
