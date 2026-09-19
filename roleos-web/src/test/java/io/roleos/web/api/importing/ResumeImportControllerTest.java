package io.roleos.web.api.importing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.roleos.application.career.importing.ResumeImportApplicationService;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.importing.FactCandidate;
import io.roleos.domain.career.importing.ResumeImport;
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
      ResumeImportController.class,
      SecurityConfiguration.class,
      SecurityErrorWriter.class,
      GlobalExceptionHandler.class,
      TraceIdFilter.class
    })
class ResumeImportControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private ResumeImportApplicationService service;
  @MockitoBean private CurrentUserResolver currentUser;

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void createsWaitingCandidatesAndKeepsPartialFailuresVisible() throws Exception {
    UserId userId = new UserId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    when(currentUser.requireCurrentUser()).thenReturn(userId);
    FactCandidate candidate =
        new FactCandidate(
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            "SKILL",
            "{\"displayName\":\"Java\"}",
            "skills:1");
    when(service.create(any(), any()))
        .thenReturn(
            new ResumeImportApplicationService.CreateResult(
                new ResumeImport(
                    UUID.fromString("30000000-0000-0000-0000-000000000001"),
                    userId,
                    "fixture://resume/test",
                    Instant.EPOCH,
                    List.of(candidate)),
                List.of("fixture 1 is missing payload")));

    mockMvc
        .perform(
            post("/api/v1/resume-imports")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"commandId\":\"40000000-0000-0000-0000-000000000001\",\"controlledSourceReference\":\"fixture://resume/test\",\"fixtures\":[{\"candidateType\":\"SKILL\",\"payload\":\"{\\\"displayName\\\":\\\"Java\\\"}\",\"sourceLocation\":\"skills:1\"}]}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.candidates[0].status").value("WAITING_CONFIRMATION"))
        .andExpect(jsonPath("$.data.failures[0]").value("fixture 1 is missing payload"));
  }
}
