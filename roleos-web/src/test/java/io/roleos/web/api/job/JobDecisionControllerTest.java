package io.roleos.web.api.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobSearch.JobDecision;
import io.roleos.job.domain.JobTypes.DecisionType;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.SemanticAnalysisStatus;
import io.roleos.job.domain.JobTypes.StrategyRecommendation;
import io.roleos.job.service.JobStrategyService;
import io.roleos.web.error.GlobalExceptionHandler;
import io.roleos.web.security.CurrentUserResolver;
import io.roleos.web.security.SecurityConfiguration;
import io.roleos.web.security.SecurityErrorWriter;
import io.roleos.web.trace.TraceIdFilter;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Job 决策 HTTP 契约验证：认证、幂等头、稳定响应与安全错误体。 */
@WebMvcTest
@ContextConfiguration(
    classes = {
      JobDecisionController.class,
      SecurityConfiguration.class,
      SecurityErrorWriter.class,
      GlobalExceptionHandler.class,
      TraceIdFilter.class
    })
class JobDecisionControllerTest {

  private static final UserId USER_ID =
      new UserId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final UUID JOB_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID CANDIDATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final Instant NOW = Instant.parse("2026-09-19T08:00:00Z");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private JobStrategyService service;
  @MockitoBean private CurrentUserResolver currentUser;

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void promotesCandidateAndReturnsEffectiveStrategy() throws Exception {
    when(currentUser.requireCurrentUser()).thenReturn(USER_ID);
    when(service.decide(
            eq(USER_ID), eq(JOB_ID), eq("command-0001"), eq(DecisionType.PROMOTE), any()))
        .thenReturn(
            result(DecisionType.PROMOTE, JobStrategy.BROAD_APPLY, JobStrategy.TARGETED_APPLY));

    mockMvc
        .perform(
            post("/api/jobs/{jobId}/decisions", JOB_ID)
                .with(csrf())
                .header("Idempotency-Key", "command-0001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PROMOTE\",\"reason\":\"重点岗位\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.jobId").value(JOB_ID.toString()))
        .andExpect(jsonPath("$.data.previousStrategy").value("BROAD_APPLY"))
        .andExpect(jsonPath("$.data.strategy").value("TARGETED_APPLY"));
  }

  @Test
  @WithMockUser
  void requiresIdempotencyKey() throws Exception {
    mockMvc
        .perform(
            post("/api/jobs/{jobId}/decisions", JOB_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"SKIP\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser
  void mapsNotFoundAndConflictToStableErrors() throws Exception {
    when(currentUser.requireCurrentUser()).thenReturn(USER_ID);
    when(service.decide(eq(USER_ID), eq(JOB_ID), anyString(), eq(DecisionType.SKIP), any()))
        .thenThrow(
            new JobStrategyService.JobStrategyException(
                JobStrategyService.FailureReason.NOT_FOUND, "missing"));

    mockMvc
        .perform(
            post("/api/jobs/{jobId}/decisions", JOB_ID)
                .with(csrf())
                .header("Idempotency-Key", "command-0002")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"SKIP\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));

    when(service.decide(eq(USER_ID), eq(JOB_ID), anyString(), eq(DecisionType.PROMOTE), any()))
        .thenThrow(
            new JobStrategyService.JobStrategyException(
                JobStrategyService.FailureReason.CONFLICT, "conflict"));
    mockMvc
        .perform(
            post("/api/jobs/{jobId}/decisions", JOB_ID)
                .with(csrf())
                .header("Idempotency-Key", "command-0003")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PROMOTE\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("JOB_DECISION_CONFLICT"));
  }

  @Test
  void rejectsUnauthenticatedDecision() throws Exception {
    mockMvc
        .perform(
            post("/api/jobs/{jobId}/decisions", JOB_ID)
                .with(csrf())
                .header("Idempotency-Key", "command-0004")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"SKIP\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
  }

  private JobStrategyService.DecisionResult result(
      DecisionType type, JobStrategy previous, JobStrategy resulting) {
    JobDecision decision =
        new JobDecision(
            UUID.randomUUID(),
            CANDIDATE_ID,
            USER_ID,
            "command-0001",
            type,
            previous,
            resulting,
            null,
            NOW);
    JobCandidate candidate =
        new JobCandidate(
            CANDIDATE_ID,
            USER_ID,
            JOB_ID,
            FilterResult.PASS,
            null,
            resulting,
            StrategyRecommendation.PROMOTE_TO_TARGETED,
            SemanticAnalysisStatus.SUCCEEDED,
            null,
            Map.of(),
            1,
            NOW,
            NOW);
    return new JobStrategyService.DecisionResult(decision, candidate);
  }
}
