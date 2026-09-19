package io.roleos.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.roleos.application.career.importing.ResumeImportApplicationService;
import io.roleos.application.career.profile.CareerProfileApplicationService;
import io.roleos.application.career.skill.SkillProvenanceApplicationService;
import io.roleos.application.career.workflow.WorkflowApplicationService;
import io.roleos.domain.career.UserId;
import io.roleos.web.api.career.CareerProfileController;
import io.roleos.web.api.importing.ResumeImportController;
import io.roleos.web.api.skill.SkillController;
import io.roleos.web.api.workflow.WorkflowController;
import io.roleos.web.error.GlobalExceptionHandler;
import io.roleos.web.security.CurrentUserResolver;
import io.roleos.web.security.SecurityConfiguration;
import io.roleos.web.security.SecurityErrorWriter;
import io.roleos.web.trace.TraceIdFilter;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest
@ContextConfiguration(
    classes = {
      CareerProfileController.class,
      ResumeImportController.class,
      SkillController.class,
      WorkflowController.class,
      SecurityConfiguration.class,
      SecurityErrorWriter.class,
      GlobalExceptionHandler.class,
      TraceIdFilter.class
    })
class CrossCuttingApiContractTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private CurrentUserResolver currentUser;
  @MockitoBean private CareerProfileApplicationService profiles;
  @MockitoBean private ResumeImportApplicationService imports;
  @MockitoBean private SkillProvenanceApplicationService skills;
  @MockitoBean private WorkflowApplicationService workflows;

  @ParameterizedTest
  @MethodSource("featureEndpoints")
  void rejectsUnauthenticatedFeatureEndpointsWithAUniformTraceableError(
      MockHttpServletRequestBuilder request) throws Exception {
    mockMvc
        .perform(request)
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists(TraceIdFilter.HEADER_NAME))
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void returnsTraceableValidationAndRedactedUnexpectedErrors() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/career-profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());

    when(currentUser.requireCurrentUser())
        .thenReturn(new UserId(UUID.fromString("10000000-0000-0000-0000-000000000001")));
    when(profiles.get(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new RuntimeException("jdbc password=secret"));
    mockMvc
        .perform(get("/api/v1/career-profile"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("服务暂时不可用，请稍后重试"))
        .andExpect(
            jsonPath("$.message")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  private static Stream<MockHttpServletRequestBuilder> featureEndpoints() {
    return Stream.of(
        get("/api/v1/career-profile"),
        get("/api/v1/skills"),
        get("/api/v1/workflows"),
        get("/api/v1/approvals"),
        post("/api/v1/resume-imports")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"));
  }
}
