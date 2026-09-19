package io.roleos.web.api.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.roleos.application.career.workflow.WorkflowApplicationService;
import io.roleos.domain.career.UserId;
import io.roleos.domain.workflow.WorkflowInstance;
import io.roleos.domain.workflow.WorkflowStage;
import io.roleos.domain.workflow.WorkflowStatus;
import io.roleos.domain.workflow.WorkflowType;
import io.roleos.web.error.GlobalExceptionHandler;
import io.roleos.web.security.CurrentUserResolver;
import io.roleos.web.security.SecurityConfiguration;
import io.roleos.web.security.SecurityErrorWriter;
import io.roleos.web.trace.TraceIdFilter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(
    classes = {
      WorkflowController.class,
      SecurityConfiguration.class,
      SecurityErrorWriter.class,
      GlobalExceptionHandler.class,
      TraceIdFilter.class
    })
class WorkflowControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private WorkflowApplicationService service;
  @MockitoBean private CurrentUserResolver currentUser;

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void createsWorkflowWithRequiredCommandId() throws Exception {
    UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    when(currentUser.requireCurrentUser()).thenReturn(new UserId(userId));
    when(service.create(any(), any(), any(), any(), any())).thenReturn(workflow(userId));

    mockMvc
        .perform(
            post("/api/v1/workflows")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"commandId\":\"create-1\",\"type\":\"TARGETED_APPLY\",\"stage\":\"APPLICATION_APPROVAL\",\"waitingReason\":\"等待确认\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.status").value("WAITING_APPROVAL"));
    verify(service)
        .create(
            userId,
            WorkflowType.TARGETED_APPLY,
            WorkflowStage.APPLICATION_APPROVAL,
            "等待确认",
            "create-1");
  }

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void readsWorkflowsAndApprovalsOnlyForCurrentUser() throws Exception {
    UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    when(currentUser.requireCurrentUser()).thenReturn(new UserId(userId));
    when(service.list(userId)).thenReturn(List.of(workflow(userId)));
    when(service.approvals(userId)).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/workflows")).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/approvals")).andExpect(status().isOk());
    verify(service).list(userId);
    verify(service).approvals(userId);
  }

  private static WorkflowInstance workflow(UUID userId) {
    return new WorkflowInstance(
        UUID.randomUUID(),
        userId,
        WorkflowType.TARGETED_APPLY,
        WorkflowStage.APPLICATION_APPROVAL,
        WorkflowStatus.WAITING_APPROVAL,
        "等待确认",
        0,
        0,
        Instant.EPOCH,
        Instant.EPOCH);
  }
}
