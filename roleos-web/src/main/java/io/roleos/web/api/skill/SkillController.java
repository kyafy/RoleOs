package io.roleos.web.api.skill;

import io.roleos.application.career.skill.SkillProvenanceApplicationService;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.skill.SkillSourceType;
import io.roleos.web.api.ApiResponse;
import io.roleos.web.security.CurrentUserResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Skill facts expose self assessment, verified level and sources as distinct values. */
@RestController
@RequestMapping("/api/v1")
public class SkillController {
  private final SkillProvenanceApplicationService service;
  private final CurrentUserResolver currentUser;

  public SkillController(
      SkillProvenanceApplicationService service, CurrentUserResolver currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping("/skills")
  ApiResponse<List<SkillProvenanceApplicationService.SkillView>> skills() {
    return ApiResponse.success(service.skills(currentUser.requireCurrentUser()), trace());
  }

  @GetMapping("/capabilities")
  ApiResponse<List<SkillProvenanceApplicationService.CapabilityView>> capabilities() {
    return ApiResponse.success(service.capabilities(currentUser.requireCurrentUser()), trace());
  }

  @PostMapping("/skills/sources")
  ApiResponse<?> source(@Valid @RequestBody SourceRequest r) {
    return ApiResponse.success(
        service.addSource(
            currentUser.requireCurrentUser(),
            new SkillProvenanceApplicationService.AddSourceCommand(
                new CommandId(r.commandId()), r.skillId(), r.type(), r.supportingAssetReference())),
        trace());
  }

  @PostMapping("/capabilities")
  ApiResponse<?> capability(@Valid @RequestBody CapabilityRequest r) {
    return ApiResponse.success(
        service.saveCapability(
            currentUser.requireCurrentUser(),
            new SkillProvenanceApplicationService.SaveCapabilityCommand(
                new CommandId(r.commandId()),
                r.id(),
                r.name(),
                r.skillIds(),
                r.experienceIds(),
                r.projectIds())),
        trace());
  }

  private String trace() {
    return java.util.Objects.requireNonNullElse(MDC.get("traceId"), "unknown");
  }

  record SourceRequest(
      @NotNull UUID commandId,
      @NotNull UUID skillId,
      @NotNull SkillSourceType type,
      @NotBlank String supportingAssetReference) {}

  record CapabilityRequest(
      @NotNull UUID commandId,
      UUID id,
      @NotBlank String name,
      List<UUID> skillIds,
      List<UUID> experienceIds,
      List<UUID> projectIds) {
    CapabilityRequest {
      skillIds = skillIds == null ? List.of() : List.copyOf(skillIds);
      experienceIds = experienceIds == null ? List.of() : List.copyOf(experienceIds);
      projectIds = projectIds == null ? List.of() : List.copyOf(projectIds);
    }
  }
}
