package io.roleos.web.api.job;

import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobSearch.JobDecision;
import io.roleos.job.domain.JobTypes.DecisionType;
import io.roleos.job.service.JobStrategyService;
import io.roleos.job.service.JobStrategyService.JobStrategyException;
import io.roleos.web.api.ApiResponse;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.error.RoleOsException;
import io.roleos.web.security.CurrentUserResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 接收用户对 Job Candidate 的显式策略决定；推荐不会经过此接口自动执行。 */
@Validated
@RestController
@RequestMapping("/api/jobs")
public class JobDecisionController {

  private final JobStrategyService service;
  private final CurrentUserResolver currentUser;

  public JobDecisionController(JobStrategyService service, CurrentUserResolver currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping("/{jobId}/decisions")
  ApiResponse<JobDecisionResponse> decide(
      @PathVariable UUID jobId,
      @RequestHeader("Idempotency-Key") @Size(min = 8, max = 128) String commandId,
      @Valid @RequestBody JobDecisionRequest request) {
    try {
      var result =
          service.decide(
              currentUser.requireCurrentUser(),
              jobId,
              commandId,
              request.decision(),
              request.reason());
      return ApiResponse.success(
          JobDecisionResponse.from(jobId, result.decision(), result.candidate()), traceId());
    } catch (JobStrategyException exception) {
      if (exception.reason() == JobStrategyService.FailureReason.NOT_FOUND) {
        throw new RoleOsException(
            ErrorCode.JOB_NOT_FOUND, HttpStatus.NOT_FOUND, "当前用户无此岗位候选", exception);
      }
      throw new RoleOsException(
          ErrorCode.JOB_DECISION_CONFLICT, HttpStatus.CONFLICT, "岗位策略已被其他请求修改", exception);
    }
  }

  private String traceId() {
    return Objects.requireNonNullElse(MDC.get("traceId"), "unknown");
  }

  record JobDecisionRequest(@NotNull DecisionType decision, @Size(max = 500) String reason) {}

  record JobDecisionResponse(
      UUID jobId,
      DecisionType decision,
      String previousStrategy,
      String strategy,
      Instant decidedAt) {
    static JobDecisionResponse from(UUID jobId, JobDecision decision, JobCandidate candidate) {
      return new JobDecisionResponse(
          jobId,
          decision.decision(),
          decision.previousStrategy().name(),
          candidate.strategy().name(),
          decision.decidedAt());
    }
  }
}
