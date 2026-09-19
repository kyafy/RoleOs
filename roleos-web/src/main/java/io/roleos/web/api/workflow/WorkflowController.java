package io.roleos.web.api.workflow;

import io.roleos.application.career.workflow.WorkflowApplicationService;
import io.roleos.domain.workflow.*;
import io.roleos.web.api.ApiResponse;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.error.RoleOsException;
import io.roleos.web.security.CurrentUserResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@SuppressWarnings("PMD.PreserveStackTrace")
public class WorkflowController {
  private final WorkflowApplicationService service;
  private final CurrentUserResolver users;

  public WorkflowController(WorkflowApplicationService service, CurrentUserResolver users) {
    this.service = service;
    this.users = users;
  }

  @GetMapping("/workflows")
  ApiResponse<?> list() {
    return ApiResponse.success(service.list(users.requireCurrentUser().value()), trace());
  }

  @PostMapping("/workflows")
  @ResponseStatus(HttpStatus.ACCEPTED)
  ApiResponse<?> create(@Valid @RequestBody Create r) {
    return ApiResponse.success(
        service.create(
            users.requireCurrentUser().value(),
            r.type(),
            r.stage(),
            r.waitingReason(),
            r.commandId()),
        trace());
  }

  @GetMapping("/approvals")
  ApiResponse<?> approvals() {
    return ApiResponse.success(service.approvals(users.requireCurrentUser().value()), trace());
  }

  @PostMapping("/approvals/{id}/decisions")
  ApiResponse<?> decide(@PathVariable UUID id, @Valid @RequestBody Decision r) {
    try {
      return ApiResponse.success(
          service.decide(users.requireCurrentUser().value(), id, r.decision(), r.commandId()),
          trace());
    } catch (IllegalStateException exception) {
      throw conflict();
    }
  }

  @PostMapping("/workflows/{id}/resume")
  ApiResponse<?> resume(@PathVariable UUID id, @Valid @RequestBody Command r) {
    try {
      return ApiResponse.success(
          service.resume(users.requireCurrentUser().value(), id, r.commandId()), trace());
    } catch (IllegalStateException exception) {
      throw conflict();
    }
  }

  @PostMapping("/workflows/{id}/cancel")
  ApiResponse<?> cancel(@PathVariable UUID id, @Valid @RequestBody Command r) {
    try {
      return ApiResponse.success(
          service.cancel(users.requireCurrentUser().value(), id, r.commandId()), trace());
    } catch (IllegalStateException exception) {
      throw conflict();
    }
  }

  private RoleOsException conflict() {
    return new RoleOsException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "工作流当前状态不允许此操作");
  }

  private String trace() {
    return Objects.requireNonNullElse(MDC.get("traceId"), "unknown");
  }

  record Create(
      @NotBlank String commandId,
      @NotNull WorkflowType type,
      @NotNull WorkflowStage stage,
      String waitingReason) {}

  record Decision(@NotBlank String commandId, @NotNull ApprovalDecision decision) {}

  record Command(@NotBlank String commandId) {}
}
