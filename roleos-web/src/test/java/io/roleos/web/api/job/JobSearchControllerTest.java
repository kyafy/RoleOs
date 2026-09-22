package io.roleos.web.api.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobSearch;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.SearchState;
import io.roleos.job.domain.JobTypes.SearchStep;
import io.roleos.job.service.JobSearchOrchestrator;
import io.roleos.job.service.JobSearchOrchestrator.FailureReason;
import io.roleos.job.service.JobSearchOrchestrator.JobSearchException;
import io.roleos.web.error.GlobalExceptionHandler;
import io.roleos.web.security.CurrentUserResolver;
import io.roleos.web.security.SecurityConfiguration;
import io.roleos.web.security.SecurityErrorWriter;
import io.roleos.web.trace.TraceIdFilter;
import java.net.URI;
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

/** 岗位搜索 HTTP 合同：主动启动、状态读取、人工恢复与安全冲突。 */
@WebMvcTest
@ContextConfiguration(
    classes = {
      JobSearchController.class,
      SecurityConfiguration.class,
      SecurityErrorWriter.class,
      GlobalExceptionHandler.class,
      TraceIdFilter.class
    })
class JobSearchControllerTest {

  private static final UserId USER = UserId.random();
  private static final UUID SEARCH_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-09-19T08:00:00Z");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private JobSearchOrchestrator orchestrator;
  @MockitoBean private CurrentUserResolver currentUser;

  @Test
  @WithMockUser
  void acceptsSearchAndReturnsCompletedState() throws Exception {
    when(currentUser.requireCurrentUser()).thenReturn(USER);
    when(orchestrator.start(eq(USER), eq("search-command-001"), any(), any()))
        .thenReturn(search(SearchState.COMPLETED));

    mockMvc
        .perform(
            post("/api/job-searches")
                .with(csrf())
                .header("Idempotency-Key", "search-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"keywords":["AI Agent"],"city":"上海","salaryMin":30,
                     "experienceYears":5,"targetRole":"AI Agent 工程师","provider":"AUTO"}
                    """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.id").value(SEARCH_ID.toString()))
        .andExpect(jsonPath("$.data.state").value("COMPLETED"))
        .andExpect(jsonPath("$.data.counts.discovered").value(3))
        .andExpect(jsonPath("$.data.counts.ranked").value(1));
  }

  @Test
  @WithMockUser
  void mapsMissingSearchAndResumeConflict() throws Exception {
    when(currentUser.requireCurrentUser()).thenReturn(USER);
    when(orchestrator.find(USER, SEARCH_ID))
        .thenThrow(new JobSearchException(FailureReason.NOT_FOUND, "missing"));
    mockMvc
        .perform(get("/api/job-searches/{searchId}", SEARCH_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));

    when(orchestrator.resume(USER, SEARCH_ID, "resume-command-001"))
        .thenThrow(new JobSearchException(FailureReason.CONFLICT, "conflict"));
    mockMvc
        .perform(
            post("/api/job-searches/{searchId}/resume", SEARCH_ID)
                .with(csrf())
                .header("Idempotency-Key", "resume-command-001"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("JOB_SEARCH_CONFLICT"));
  }

  private JobSearch search(SearchState state) {
    return new JobSearch(
        SEARCH_ID,
        USER,
        Map.of(
            "keywords", java.util.List.of("AI Agent"), "city", "上海", "targetRole", "AI Agent 工程师"),
        "search-command-001",
        null,
        BrowserProviderType.AUTO,
        BrowserProviderType.PLAYWRIGHT_MCP,
        state,
        SearchStep.RANK,
        URI.create("https://www.zhipin.com/web/geek/job"),
        null,
        null,
        null,
        0,
        new JobSearch.Counts(3, 2, 1, 1),
        NOW,
        NOW,
        state == SearchState.COMPLETED ? NOW : null);
  }
}
