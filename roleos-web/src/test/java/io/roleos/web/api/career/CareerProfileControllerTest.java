package io.roleos.web.api.career;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.roleos.application.career.profile.CareerProfileApplicationService;
import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.profile.CareerProfile;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.error.GlobalExceptionHandler;
import io.roleos.web.error.RoleOsException;
import io.roleos.web.security.CurrentUserResolver;
import io.roleos.web.security.SecurityConfiguration;
import io.roleos.web.security.SecurityErrorWriter;
import io.roleos.web.trace.TraceIdFilter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(
    classes = {
      CareerProfileController.class,
      SecurityConfiguration.class,
      SecurityErrorWriter.class,
      GlobalExceptionHandler.class,
      TraceIdFilter.class
    })
class CareerProfileControllerTest {
  private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CareerProfileApplicationService service;
  @MockitoBean private CurrentUserResolver currentUserResolver;

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void returnsProfileWithoutOwnershipOrAuditFields() throws Exception {
    when(currentUserResolver.requireCurrentUser()).thenReturn(new UserId(USER_ID));
    when(service.get(any())).thenReturn(profile());

    mockMvc
        .perform(get("/api/v1/career-profile").header(TraceIdFilter.HEADER_NAME, "trace-profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName").value("测试用户"))
        .andExpect(jsonPath("$.data.userId").doesNotExist())
        .andExpect(jsonPath("$.data.auditFields").doesNotExist())
        .andExpect(jsonPath("$.traceId").value("trace-profile"));
  }

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void validatesProfileWriteRequest() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/career-profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"\",\"careerDirection\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void rejectsCrossUserAccess() throws Exception {
    when(currentUserResolver.requireCurrentUser())
        .thenThrow(
            new RoleOsException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, "当前身份无权访问该资源"));

    mockMvc.perform(get("/api/v1/career-profile")).andExpect(status().isForbidden());
  }

  private CareerProfile profile() {
    return new CareerProfile(
        UUID.fromString("20000000-0000-0000-0000-000000000001"),
        new UserId(USER_ID),
        "测试用户",
        "AI Agent 工程",
        "后端工程师",
        ProvenanceType.USER_INPUT,
        ConfirmationStatus.CONFIRMED,
        new AuditFields(Instant.EPOCH, Instant.EPOCH, 0));
  }
}
