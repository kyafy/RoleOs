package io.roleos.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.roleos.application.career.workflow.WorkflowApplicationService;
import io.roleos.domain.workflow.ApprovalDecision;
import io.roleos.domain.workflow.ApprovalStatus;
import io.roleos.domain.workflow.WorkflowStage;
import io.roleos.domain.workflow.WorkflowStatus;
import io.roleos.domain.workflow.WorkflowType;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

class WorkflowRecoveryIT {
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
  void restoresApprovalAcrossRestartAndAdvancesAtMostOnce() throws Exception {
    UUID owner = UUID.randomUUID();
    UUID otherUser = UUID.randomUUID();
    UUID workflowId;
    UUID approvalId;

    try (ConfigurableApplicationContext first = startApplication()) {
      WorkflowApplicationService workflows = first.getBean(WorkflowApplicationService.class);
      workflowId =
          workflows
              .create(
                  owner,
                  WorkflowType.TARGETED_APPLY,
                  WorkflowStage.APPLICATION_APPROVAL,
                  "等待最终投递确认",
                  "create-workflow")
              .id();
      approvalId = workflows.approvals(owner).getFirst().id();
      assertThat(workflows.list(owner))
          .singleElement()
          .satisfies(
              workflow -> {
                assertThat(workflow.status()).isEqualTo(WorkflowStatus.WAITING_APPROVAL);
                assertThat(workflow.waitingReason()).isEqualTo("等待最终投递确认");
              });
      assertThat(workflows.approvals(otherUser)).isEmpty();
      assertThatThrownBy(
              () -> workflows.decide(otherUser, approvalId, ApprovalDecision.APPROVE, "foreign"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    try (ConfigurableApplicationContext second = startApplication()) {
      WorkflowApplicationService workflows = second.getBean(WorkflowApplicationService.class);
      assertThat(workflows.list(owner)).singleElement().extracting("id").isEqualTo(workflowId);
      assertThat(workflows.approvals(owner))
          .singleElement()
          .extracting("status")
          .isEqualTo(ApprovalStatus.PENDING);

      var first = workflows.decide(owner, approvalId, ApprovalDecision.APPROVE, "approve-once");
      var duplicate = workflows.decide(owner, approvalId, ApprovalDecision.APPROVE, "approve-once");
      assertThat(duplicate).isEqualTo(first);
      assertThat(first.status()).isEqualTo(WorkflowStatus.RUNNING);
      assertThat(first.version()).isEqualTo(1);
    }

    try (ConfigurableApplicationContext third = startApplication();
        ExecutorService workers = Executors.newFixedThreadPool(2)) {
      WorkflowApplicationService workflows = third.getBean(WorkflowApplicationService.class);
      UUID concurrentWorkflow =
          workflows
              .create(
                  owner,
                  WorkflowType.PROJECT_UPGRADE,
                  WorkflowStage.PROJECT_UPGRADE,
                  "重大项目升级须确认",
                  "create-concurrent-workflow")
              .id();
      UUID concurrentApproval =
          workflows.approvals(owner).stream()
              .filter(approval -> approval.workflowId().equals(concurrentWorkflow))
              .findFirst()
              .orElseThrow()
              .id();
      Callable<WorkflowStatus> firstDecision =
          () ->
              workflows
                  .decide(
                      owner, concurrentApproval, ApprovalDecision.APPROVE, "approve-concurrent-1")
                  .status();
      Callable<WorkflowStatus> secondDecision =
          () ->
              workflows
                  .decide(
                      owner, concurrentApproval, ApprovalDecision.APPROVE, "approve-concurrent-2")
                  .status();

      var results = workers.invokeAll(java.util.List.of(firstDecision, secondDecision));
      var outcomes =
          results.stream()
              .map(
                  result -> {
                    try {
                      return result.get().name();
                    } catch (Exception exception) {
                      return exception.getCause().toString();
                    }
                  })
              .toList();
      assertThat(outcomes).anyMatch(outcome -> outcome.equals("RUNNING"));
      assertThat(workflows.list(owner).stream().filter(w -> w.id().equals(concurrentWorkflow)))
          .singleElement()
          .extracting("status")
          .isEqualTo(WorkflowStatus.RUNNING);
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
}
