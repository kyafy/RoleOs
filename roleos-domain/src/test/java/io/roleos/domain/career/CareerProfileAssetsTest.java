package io.roleos.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.roleos.domain.career.profile.Project;
import io.roleos.domain.career.profile.ProjectNature;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import io.roleos.domain.career.skill.Skill;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareerProfileAssetsTest {

  private static final UserId USER = UserId.random();
  private static final AuditFields AUDIT = new AuditFields(Instant.EPOCH, Instant.EPOCH, 0);

  @Test
  void projectAlwaysRetainsOwningUserAndExplicitNature() {
    Project project =
        new Project(
            UUID.randomUUID(),
            USER,
            "RoleOS",
            ProjectNature.PERSONAL_PORTFOLIO,
            "用户个人项目",
            ProvenanceType.USER_INPUT,
            ConfirmationStatus.CONFIRMED,
            AUDIT);

    assertThat(project.userId()).isEqualTo(USER);
    assertThat(project.nature()).isEqualTo(ProjectNature.PERSONAL_PORTFOLIO);
  }

  @Test
  void skillNameMustMatchNormalizedFormAndKeepAssessmentSeparate() {
    assertThat(Skill.normalize("  Spring   Boot ")).isEqualTo("spring boot");
    assertThatThrownBy(
            () ->
                new Skill(
                    UUID.randomUUID(),
                    USER,
                    "Spring Boot",
                    "springboot",
                    SelfAssessmentLevel.WORKING,
                    null,
                    ProvenanceType.USER_INPUT,
                    ConfirmationStatus.CONFIRMED,
                    AUDIT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void agentInferenceCannotBecomeConfirmedCareerFact() {
    CareerFact inference =
        new CareerFact() {
          @Override
          public UserId userId() {
            return USER;
          }

          @Override
          public ProvenanceType provenanceType() {
            return ProvenanceType.AGENT_INFERENCE;
          }

          @Override
          public ConfirmationStatus confirmationStatus() {
            return ConfirmationStatus.CONFIRMED;
          }
        };

    assertThatThrownBy(inference::requireEligibleForFactStore)
        .isInstanceOf(IllegalStateException.class);
  }
}
