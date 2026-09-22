package io.roleos.web.api.job;

import io.roleos.job.domain.JobSearch;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.port.JobSourcePort.JobSearchCriteria;
import io.roleos.job.port.SemanticRankingPort.RankingContext;
import io.roleos.job.service.JobSearchOrchestrator;
import io.roleos.job.service.JobSearchOrchestrator.JobSearchException;
import io.roleos.web.api.ApiResponse;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.error.RoleOsException;
import io.roleos.web.security.CurrentUserResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 用户主动发起、查看和恢复低频岗位搜索的 HTTP 边界。 */
@Validated
@RestController
@RequestMapping("/api/job-searches")
public class JobSearchController {

  private final JobSearchOrchestrator orchestrator;
  private final CurrentUserResolver currentUser;

  public JobSearchController(JobSearchOrchestrator orchestrator, CurrentUserResolver currentUser) {
    this.orchestrator = orchestrator;
    this.currentUser = currentUser;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  ApiResponse<JobSearchResponse> start(
      @RequestHeader("Idempotency-Key") @Size(min = 8, max = 128) String commandId,
      @Valid @RequestBody StartJobSearchRequest request) {
    JobSearch search =
        orchestrator.start(
            currentUser.requireCurrentUser(),
            commandId,
            request.criteria(),
            request.rankingContext());
    return ApiResponse.success(JobSearchResponse.from(search), traceId());
  }

  @GetMapping("/{searchId}")
  ApiResponse<JobSearchResponse> get(@PathVariable UUID searchId) {
    return ApiResponse.success(JobSearchResponse.from(find(searchId)), traceId());
  }

  @PostMapping("/{searchId}/resume")
  @ResponseStatus(HttpStatus.ACCEPTED)
  ApiResponse<JobSearchResponse> resume(
      @PathVariable UUID searchId,
      @RequestHeader("Idempotency-Key") @Size(min = 8, max = 128) String commandId) {
    try {
      return ApiResponse.success(
          JobSearchResponse.from(
              orchestrator.resume(currentUser.requireCurrentUser(), searchId, commandId)),
          traceId());
    } catch (JobSearchException exception) {
      throw map(exception);
    }
  }

  private JobSearch find(UUID searchId) {
    try {
      return orchestrator.find(currentUser.requireCurrentUser(), searchId);
    } catch (JobSearchException exception) {
      throw map(exception);
    }
  }

  private static RoleOsException map(JobSearchException exception) {
    if (exception.reason() == JobSearchOrchestrator.FailureReason.NOT_FOUND) {
      return new RoleOsException(ErrorCode.JOB_NOT_FOUND, HttpStatus.NOT_FOUND, "当前用户无此岗位搜索");
    }
    return new RoleOsException(ErrorCode.JOB_SEARCH_CONFLICT, HttpStatus.CONFLICT, "当前岗位搜索状态不可恢复");
  }

  private String traceId() {
    return Objects.requireNonNullElse(MDC.get("traceId"), "unknown");
  }

  record StartJobSearchRequest(
      @NotEmpty @Size(max = 5) List<@NotBlank @Size(max = 50) String> keywords,
      @NotBlank @Size(max = 50) String city,
      @Min(0) Integer salaryMin,
      @Min(0) @Max(60) Integer experienceYears,
      @NotBlank @Size(max = 100) String targetRole,
      @NotNull BrowserProviderType provider) {

    JobSearchCriteria criteria() {
      return new JobSearchCriteria(
          keywords, city, salaryMin, experienceYears, targetRole, provider);
    }

    RankingContext rankingContext() {
      return new RankingContext(targetRole, List.of(), null);
    }
  }

  record Counts(int discovered, int normalized, int rejected, int ranked) {}

  record JobSearchResponse(
      UUID id,
      String state,
      String step,
      String requestedProvider,
      String activeProvider,
      String waitReason,
      String failureCode,
      Counts counts,
      Instant updatedAt) {

    static JobSearchResponse from(JobSearch search) {
      return new JobSearchResponse(
          search.id(),
          search.state().name(),
          search.step().name(),
          search.requestedProvider().name(),
          search.activeProvider() == null ? null : search.activeProvider().name(),
          search.waitReason(),
          search.failureCode(),
          new Counts(
              search.counts().discovered(),
              search.counts().normalized(),
              search.counts().rejected(),
              search.counts().ranked()),
          search.updatedAt());
    }
  }
}
