package io.roleos.domain.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary; callers must always supply the owning user id. */
public interface WorkflowRepository {
  WorkflowInstance create(WorkflowInstance workflow, Approval approval, String commandId);

  List<WorkflowInstance> findAll(UUID userId);

  Optional<WorkflowInstance> find(UUID userId, UUID workflowId);

  /** Returns the original command result, scoped through the workflow owner. */
  Optional<WorkflowInstance> findByCommandId(UUID userId, String commandId);

  Optional<Approval> findApproval(UUID userId, UUID approvalId);

  List<Approval> findApprovals(UUID userId);

  WorkflowInstance saveTransition(WorkflowInstance workflow, Approval approval, String commandId);

  WorkflowInstance saveState(
      WorkflowInstance workflow, WorkflowStatus previousStatus, String commandId);
}
