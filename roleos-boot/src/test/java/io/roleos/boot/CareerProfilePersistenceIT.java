package io.roleos.boot;

import static org.assertj.core.api.Assertions.assertThat;

import io.roleos.application.career.profile.CareerAssetApplicationService;
import io.roleos.application.career.profile.CareerProfileApplicationService;
import io.roleos.application.career.profile.UpsertCareerProfileCommand;
import io.roleos.domain.career.CommandId;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.profile.ExperienceType;
import io.roleos.domain.career.profile.ProjectNature;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

class CareerProfilePersistenceIT {
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
  void restoresProfileAndAssetsAfterApplicationRestartAndIsolatesUsers() {
    UserId owner = UserId.random();
    UserId otherUser = UserId.random();

    try (ConfigurableApplicationContext first = startApplication()) {
      CareerProfileApplicationService profiles =
          first.getBean(CareerProfileApplicationService.class);
      CareerAssetApplicationService assets = first.getBean(CareerAssetApplicationService.class);
      profiles.upsert(
          owner, new UpsertCareerProfileCommand(commandId(), "测试用户", "AI Agent 工程", "后端工程师"));
      assets.saveExperience(
          owner,
          new CareerAssetApplicationService.ExperienceCommand(
              commandId(), null, ExperienceType.EMPLOYMENT, "工程师", "真实组织", false));
      assets.saveProject(
          owner,
          new CareerAssetApplicationService.ProjectCommand(
              commandId(), null, "RoleOS", ProjectNature.PERSONAL_PORTFOLIO, "个人项目"));
      assets.saveSkill(
          owner,
          new CareerAssetApplicationService.SkillCommand(
              commandId(), null, "Spring Boot", SelfAssessmentLevel.WORKING));
    }

    try (ConfigurableApplicationContext second = startApplication()) {
      CareerProfileApplicationService profiles =
          second.getBean(CareerProfileApplicationService.class);
      CareerAssetApplicationService assets = second.getBean(CareerAssetApplicationService.class);
      assertThat(profiles.get(owner).displayName()).isEqualTo("测试用户");
      assertThat(assets.experiences(owner)).hasSize(1);
      assertThat(assets.projects(owner)).hasSize(1);
      assertThat(assets.skills(owner)).hasSize(1);
      assertThat(assets.experiences(otherUser)).isEmpty();
      assertThat(assets.projects(otherUser)).isEmpty();
      assertThat(assets.skills(otherUser)).isEmpty();
    }
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

  private CommandId commandId() {
    return new CommandId(UUID.randomUUID());
  }
}
