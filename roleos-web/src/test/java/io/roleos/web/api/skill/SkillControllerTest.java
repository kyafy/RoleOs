package io.roleos.web.api.skill;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.roleos.application.career.skill.SkillProvenanceApplicationService;
import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import io.roleos.domain.career.skill.Skill;
import io.roleos.domain.career.skill.SkillSource;
import io.roleos.domain.career.skill.SkillSourceType;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(
    classes = {
      SkillController.class,
      SecurityConfiguration.class,
      SecurityErrorWriter.class,
      GlobalExceptionHandler.class,
      TraceIdFilter.class
    })
class SkillControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private SkillProvenanceApplicationService service;
  @MockitoBean private CurrentUserResolver currentUser;

  @Test
  @WithMockUser(username = "10000000-0000-0000-0000-000000000001")
  void presentsNormalizedSkillWithSeparateLevelsAndSources() throws Exception {
    UserId user = new UserId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    Skill skill =
        new Skill(
            UUID.randomUUID(),
            user,
            " LangGraph ",
            "langgraph",
            SelfAssessmentLevel.USED,
            null,
            ProvenanceType.USER_INPUT,
            ConfirmationStatus.CONFIRMED,
            new AuditFields(Instant.EPOCH, Instant.EPOCH, 0));
    when(currentUser.requireCurrentUser()).thenReturn(user);
    when(service.skills(any()))
        .thenReturn(
            List.of(
                new SkillProvenanceApplicationService.SkillView(
                    skill,
                    List.of(
                        new SkillSource(
                            UUID.randomUUID(),
                            skill.id(),
                            SkillSourceType.SELF_DECLARED,
                            "skill:" + skill.id(),
                            Instant.EPOCH)))));
    mockMvc
        .perform(get("/api/v1/skills"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].skill.normalizedName").value("langgraph"))
        .andExpect(jsonPath("$.data[0].skill.selfAssessmentLevel").value("USED"))
        .andExpect(jsonPath("$.data[0].skill.verifiedLevel").isEmpty())
        .andExpect(jsonPath("$.data[0].sources[0].type").value("SELF_DECLARED"));
  }
}
