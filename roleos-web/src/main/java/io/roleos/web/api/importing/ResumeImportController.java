package io.roleos.web.api.importing;

import io.roleos.application.career.importing.ResumeImportApplicationService;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.importing.CandidateDecisionType;
import io.roleos.domain.career.importing.FactCandidate;
import io.roleos.experience.importer.ResumeImportFixture;
import io.roleos.web.api.ApiResponse;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.error.RoleOsException;
import io.roleos.web.security.CurrentUserResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API for controlled fixture imports and individual human confirmation decisions. */
@RestController
@RequestMapping("/api/v1")
@SuppressWarnings("PMD.PreserveStackTrace")
public class ResumeImportController {
  private final ResumeImportApplicationService service;
  private final CurrentUserResolver currentUser;

  public ResumeImportController(
      ResumeImportApplicationService service, CurrentUserResolver currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping("/resume-imports")
  @ResponseStatus(HttpStatus.ACCEPTED)
  ApiResponse<CreateResponse> create(@Valid @RequestBody CreateRequest request) {
    var result =
        service.create(
            currentUser.requireCurrentUser(),
            new ResumeImportApplicationService.CreateCommand(
                new CommandId(request.commandId()),
                request.controlledSourceReference(),
                request.toFixtures()));
    return ApiResponse.success(CreateResponse.from(result), traceId());
  }

  @PostMapping("/import-candidates/{candidateId}/decisions")
  ApiResponse<DecisionResponse> decide(
      @PathVariable UUID candidateId, @Valid @RequestBody DecisionRequest request) {
    try {
      var result =
          service.decide(
              currentUser.requireCurrentUser(),
              candidateId,
              new ResumeImportApplicationService.DecisionCommand(
                  new CommandId(request.commandId()), request.type(), request.editedPayload()));
      return ApiResponse.success(
          new DecisionResponse(result.candidateId(), result.status()), traceId());
    } catch (IllegalStateException exception) {
      throw new RoleOsException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "候选项当前状态不允许此操作");
    }
  }

  private String traceId() {
    return java.util.Objects.requireNonNullElse(MDC.get("traceId"), "unknown");
  }

  record CreateRequest(
      @NotNull UUID commandId,
      @NotBlank String controlledSourceReference,
      @NotEmpty List<@Valid FixtureRequest> fixtures) {
    List<ResumeImportFixture> toFixtures() {
      return fixtures.stream()
          .map(
              item ->
                  new ResumeImportFixture(
                      item.candidateType(), item.payload(), item.sourceLocation()))
          .toList();
    }
  }

  record FixtureRequest(
      @NotBlank String candidateType, String payload, @NotBlank String sourceLocation) {}

  record DecisionRequest(
      @NotNull UUID commandId, @NotNull CandidateDecisionType type, String editedPayload) {}

  record CreateResponse(UUID importId, List<CandidateResponse> candidates, List<String> failures) {
    static CreateResponse from(ResumeImportApplicationService.CreateResult result) {
      return new CreateResponse(
          result.resumeImport().id(),
          result.resumeImport().candidates().stream().map(CandidateResponse::from).toList(),
          result.failures());
    }
  }

  record CandidateResponse(
      UUID id, String candidateType, String payload, String sourceLocation, String status) {
    static CandidateResponse from(FactCandidate candidate) {
      return new CandidateResponse(
          candidate.id(),
          candidate.candidateType(),
          candidate.payload(),
          candidate.sourceLocation(),
          candidate.status().name());
    }
  }

  record DecisionResponse(UUID candidateId, String status) {}
}
