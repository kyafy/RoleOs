package io.roleos.web.api.career;

import io.roleos.application.career.profile.CareerAssetApplicationService;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.profile.Experience;
import io.roleos.domain.career.profile.ExperienceType;
import io.roleos.domain.career.profile.Project;
import io.roleos.domain.career.profile.ProjectNature;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import io.roleos.domain.career.skill.Skill;
import io.roleos.web.api.ApiResponse;
import io.roleos.web.security.CurrentUserResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户经历、项目和技能资源 API。 */
@RestController
@RequestMapping("/api/v1")
public class CareerAssetController {
  private final CareerAssetApplicationService service;
  private final CurrentUserResolver currentUser;

  public CareerAssetController(
      CareerAssetApplicationService service, CurrentUserResolver currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping("/experiences")
  ApiResponse<List<ExperienceResponse>> experiences() {
    return ApiResponse.success(
        service.experiences(currentUser.requireCurrentUser()).stream()
            .map(ExperienceResponse::from)
            .toList(),
        traceId());
  }

  @PostMapping("/experiences")
  ApiResponse<ExperienceResponse> createExperience(@Valid @RequestBody ExperienceRequest request) {
    return saveExperience(null, request);
  }

  @PutMapping("/experiences/{id}")
  ApiResponse<ExperienceResponse> updateExperience(
      @PathVariable UUID id, @Valid @RequestBody ExperienceRequest request) {
    return saveExperience(id, request);
  }

  @DeleteMapping("/experiences/{id}")
  ApiResponse<Boolean> deleteExperience(@PathVariable UUID id) {
    return ApiResponse.success(
        service.deleteExperience(currentUser.requireCurrentUser(), id), traceId());
  }

  @GetMapping("/projects")
  ApiResponse<List<ProjectResponse>> projects() {
    return ApiResponse.success(
        service.projects(currentUser.requireCurrentUser()).stream()
            .map(ProjectResponse::from)
            .toList(),
        traceId());
  }

  @PostMapping("/projects")
  ApiResponse<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
    return saveProject(null, request);
  }

  @PutMapping("/projects/{id}")
  ApiResponse<ProjectResponse> updateProject(
      @PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
    return saveProject(id, request);
  }

  @DeleteMapping("/projects/{id}")
  ApiResponse<Boolean> deleteProject(@PathVariable UUID id) {
    return ApiResponse.success(
        service.deleteProject(currentUser.requireCurrentUser(), id), traceId());
  }

  @GetMapping("/career-skills")
  ApiResponse<List<SkillResponse>> skills() {
    return ApiResponse.success(
        service.skills(currentUser.requireCurrentUser()).stream().map(SkillResponse::from).toList(),
        traceId());
  }

  @PostMapping("/career-skills")
  ApiResponse<SkillResponse> createSkill(@Valid @RequestBody SkillRequest request) {
    return saveSkill(null, request);
  }

  @PutMapping("/career-skills/{id}")
  ApiResponse<SkillResponse> updateSkill(
      @PathVariable UUID id, @Valid @RequestBody SkillRequest request) {
    return saveSkill(id, request);
  }

  @DeleteMapping("/career-skills/{id}")
  ApiResponse<Boolean> deleteSkill(@PathVariable UUID id) {
    return ApiResponse.success(
        service.deleteSkill(currentUser.requireCurrentUser(), id), traceId());
  }

  private ApiResponse<ExperienceResponse> saveExperience(UUID id, ExperienceRequest request) {
    Experience value =
        service.saveExperience(
            currentUser.requireCurrentUser(),
            new CareerAssetApplicationService.ExperienceCommand(
                new CommandId(request.commandId()),
                id,
                request.type(),
                request.title(),
                request.organization(),
                request.incomplete()));
    return ApiResponse.success(ExperienceResponse.from(value), traceId());
  }

  private ApiResponse<ProjectResponse> saveProject(UUID id, ProjectRequest request) {
    Project value =
        service.saveProject(
            currentUser.requireCurrentUser(),
            new CareerAssetApplicationService.ProjectCommand(
                new CommandId(request.commandId()),
                id,
                request.name(),
                request.nature(),
                request.historicalSource()));
    return ApiResponse.success(ProjectResponse.from(value), traceId());
  }

  private ApiResponse<SkillResponse> saveSkill(UUID id, SkillRequest request) {
    Skill value =
        service.saveSkill(
            currentUser.requireCurrentUser(),
            new CareerAssetApplicationService.SkillCommand(
                new CommandId(request.commandId()),
                id,
                request.displayName(),
                request.selfAssessmentLevel()));
    return ApiResponse.success(SkillResponse.from(value), traceId());
  }

  private String traceId() {
    return java.util.Objects.requireNonNullElse(MDC.get("traceId"), "unknown");
  }

  record ExperienceRequest(
      @NotNull UUID commandId,
      @NotNull ExperienceType type,
      @NotBlank String title,
      @NotNull String organization,
      boolean incomplete) {}

  record ProjectRequest(
      @NotNull UUID commandId,
      @NotBlank String name,
      @NotNull ProjectNature nature,
      @NotBlank String historicalSource) {}

  record SkillRequest(
      @NotNull UUID commandId,
      @NotBlank String displayName,
      @NotNull SelfAssessmentLevel selfAssessmentLevel) {}

  record ExperienceResponse(
      UUID id, ExperienceType type, String title, String organization, boolean incomplete) {
    static ExperienceResponse from(Experience v) {
      return new ExperienceResponse(v.id(), v.type(), v.title(), v.organization(), v.incomplete());
    }
  }

  record ProjectResponse(UUID id, String name, ProjectNature nature, String historicalSource) {
    static ProjectResponse from(Project v) {
      return new ProjectResponse(v.id(), v.name(), v.nature(), v.historicalSource());
    }
  }

  record SkillResponse(
      UUID id,
      String displayName,
      String normalizedName,
      SelfAssessmentLevel selfAssessmentLevel,
      SelfAssessmentLevel verifiedLevel) {
    static SkillResponse from(Skill v) {
      return new SkillResponse(
          v.id(), v.displayName(), v.normalizedName(), v.selfAssessmentLevel(), v.verifiedLevel());
    }
  }
}
