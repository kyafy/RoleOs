package io.roleos.boot;

import static org.assertj.core.api.Assertions.assertThat;

import io.roleos.application.career.importing.ResumeImportApplicationService;
import io.roleos.application.career.profile.CareerAssetApplicationService;
import io.roleos.application.career.profile.CareerProfileApplicationService;
import io.roleos.application.career.profile.UpsertCareerProfileCommand;
import io.roleos.application.career.skill.SkillProvenanceApplicationService;
import io.roleos.application.career.workflow.WorkflowApplicationService;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.importing.CandidateDecisionType;
import io.roleos.domain.career.profile.ExperienceType;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import io.roleos.domain.career.skill.SkillSourceType;
import io.roleos.domain.workflow.ApprovalDecision;
import io.roleos.domain.workflow.WorkflowStage;
import io.roleos.domain.workflow.WorkflowStatus;
import io.roleos.domain.workflow.WorkflowType;
import io.roleos.experience.importer.ResumeImportFixture;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Golden scenario uses only controlled fixtures; it never claims unconfirmed candidates as facts.
 */
class CareerFoundationGoldenScenarioIT {
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @BeforeAll
  static void startPostgres() {
    POSTGRES.start();
  }

  @AfterAll
  static void stopPostgres() {
    POSTGRES.stop();
  }

  @Test
  void completesCareerFoundationAcrossRestartWithoutFabricatingFacts() {
    UserId user = UserId.random();
    UUID approvalId;
    UUID workflowId;

    try (ConfigurableApplicationContext first = startApplication()) {
      CareerProfileApplicationService profiles =
          first.getBean(CareerProfileApplicationService.class);
      CareerAssetApplicationService assets = first.getBean(CareerAssetApplicationService.class);
      ResumeImportApplicationService imports = first.getBean(ResumeImportApplicationService.class);
      SkillProvenanceApplicationService skills =
          first.getBean(SkillProvenanceApplicationService.class);
      WorkflowApplicationService workflows = first.getBean(WorkflowApplicationService.class);

      profiles.upsert(
          user, new UpsertCareerProfileCommand(command(), "测试用户", "AI 应用开发", "Agent 工程师"));
      var imported =
          imports.create(
              user,
              new ResumeImportApplicationService.CreateCommand(
                  command(),
                  "fixture://golden-resume",
                  List.of(
                      new ResumeImportFixture(
                          "EXPERIENCE",
                          "{\"type\":\"PERSONAL\",\"title\":\"RoleOS 原型\",\"organization\":\"个人\",\"incomplete\":false}",
                          "experience:1"),
                      new ResumeImportFixture(
                          "SKILL",
                          "{\"displayName\":\"Java\",\"selfAssessmentLevel\":\"USED\"}",
                          "skills:1"),
                      new ResumeImportFixture(
                          "PROJECT",
                          "{\"name\":\"未确认项目\",\"nature\":\"PERSONAL_PORTFOLIO\",\"historicalSource\":\"fixture\"}",
                          "projects:1"))));
      var candidates = imported.resumeImport().candidates();
      imports.decide(user, candidates.get(0).id(), decision(CandidateDecisionType.CONFIRM, null));
      imports.decide(
          user,
          candidates.get(1).id(),
          decision(
              CandidateDecisionType.EDIT,
              "{\"displayName\":\"Java 21\",\"selfAssessmentLevel\":\"WORKING\"}"));
      imports.decide(user, candidates.get(2).id(), decision(CandidateDecisionType.REJECT, null));

      var skill = assets.skills(user).getFirst();
      skills.addSource(
          user,
          new SkillProvenanceApplicationService.AddSourceCommand(
              command(), skill.id(), SkillSourceType.SELF_DECLARED, "profile:skill"));
      skills.addSource(
          user,
          new SkillProvenanceApplicationService.AddSourceCommand(
              command(), skill.id(), SkillSourceType.CONFIRMED_EXPERIENCE, "experience:1"));
      assertThat(assets.projects(user)).isEmpty();
      assertThat(assets.experiences(user))
          .singleElement()
          .extracting("type")
          .isEqualTo(ExperienceType.PERSONAL);
      assertThat(skill.selfAssessmentLevel()).isEqualTo(SelfAssessmentLevel.WORKING);
      assertThat(skill.verifiedLevel()).isNull();

      workflowId =
          workflows
              .create(
                  user.value(),
                  WorkflowType.TARGETED_APPLY,
                  WorkflowStage.APPLICATION_APPROVAL,
                  "等待用户确认",
                  "workflow-create")
              .id();
      approvalId = workflows.approvals(user.value()).getFirst().id();
    }

    try (ConfigurableApplicationContext second = startApplication()) {
      WorkflowApplicationService workflows = second.getBean(WorkflowApplicationService.class);
      assertThat(workflows.list(user.value()))
          .singleElement()
          .satisfies(w -> assertThat(w.status()).isEqualTo(WorkflowStatus.WAITING_APPROVAL));
      var approved =
          workflows.decide(user.value(), approvalId, ApprovalDecision.APPROVE, "workflow-approve");
      assertThat(
              workflows.decide(
                  user.value(), approvalId, ApprovalDecision.APPROVE, "workflow-approve"))
          .isEqualTo(approved);
      assertThat(approved.id()).isEqualTo(workflowId);
      assertThat(approved.status()).isEqualTo(WorkflowStatus.RUNNING);
    }
  }

  private static ResumeImportApplicationService.DecisionCommand decision(
      CandidateDecisionType type, String editedPayload) {
    return new ResumeImportApplicationService.DecisionCommand(command(), type, editedPayload);
  }

  private static CommandId command() {
    return new CommandId(UUID.randomUUID());
  }

  private ConfigurableApplicationContext startApplication() {
    return new SpringApplicationBuilder(RoleOsApplication.class)
        .registerShutdownHook(false)
        .run(
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword(),
            "--server.port=0",
            "--spring.jmx.enabled=false");
  }
}
