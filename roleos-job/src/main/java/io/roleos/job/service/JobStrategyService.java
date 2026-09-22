package io.roleos.job.service;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobSearch.JobDecision;
import io.roleos.job.domain.JobTypes.DecisionType;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobDecisionRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 应用用户显式岗位策略决策；系统推荐本身永远不会调用此状态转换。 */
public final class JobStrategyService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobStrategyService.class);

  private final JobCandidateRepository candidates;
  private final JobDecisionRepository decisions;
  private final Clock clock;

  public JobStrategyService(
      JobCandidateRepository candidates, JobDecisionRepository decisions, Clock clock) {
    this.candidates = Objects.requireNonNull(candidates);
    this.decisions = Objects.requireNonNull(decisions);
    this.clock = Objects.requireNonNull(clock);
  }

  /** 执行幂等用户决策，相同 commandId 只有完全相同的请求可以安全重放。 */
  public DecisionResult decide(
      UserId userId, UUID jobId, String commandId, DecisionType type, String reason) {
    Objects.requireNonNull(userId);
    Objects.requireNonNull(jobId);
    if (commandId == null || commandId.length() < 8 || commandId.length() > 128) {
      throw new IllegalArgumentException("幂等命令标识长度必须在 8..128 之间");
    }
    Objects.requireNonNull(type);
    JobCandidate current =
        candidates
            .find(userId, jobId)
            .orElseThrow(() -> new JobStrategyException(FailureReason.NOT_FOUND, "岗位候选不存在"));
    var existing = decisions.findByCommandId(userId, commandId);
    if (existing.isPresent()) {
      JobDecision decision = existing.orElseThrow();
      if (!decision.candidateId().equals(current.id()) || decision.decision() != type) {
        throw new JobStrategyException(FailureReason.CONFLICT, "幂等命令已用于其他岗位决策");
      }
      LOGGER
          .atInfo()
          .addKeyValue("event", "job.strategy.idempotency_hit")
          .addKeyValue("jobId", jobId)
          .addKeyValue("commandId", commandId)
          .log("岗位策略决定命令已幂等命中");
      return new DecisionResult(decision, current);
    }

    JobStrategy target = resultingStrategy(type);
    var now = clock.instant();
    JobCandidate updated =
        new JobCandidate(
            current.id(),
            current.userId(),
            current.jobId(),
            current.filterResult(),
            current.rankingScore(),
            target,
            current.strategyRecommendation(),
            current.semanticAnalysisStatus(),
            reason,
            current.metadata(),
            current.version(),
            current.createdAt(),
            now);
    JobDecision decision =
        new JobDecision(
            UUID.randomUUID(),
            current.id(),
            userId,
            commandId,
            type,
            current.strategy(),
            target,
            reason,
            now);
    try {
      JobDecision saved = decisions.save(decision, updated);
      LOGGER
          .atInfo()
          .addKeyValue("event", "job.strategy.decided")
          .addKeyValue("jobId", jobId)
          .addKeyValue("candidateId", current.id())
          .addKeyValue("commandId", commandId)
          .addKeyValue("decision", type)
          .addKeyValue("fromStrategy", current.strategy())
          .addKeyValue("strategy", target)
          .log("用户岗位策略决定已持久化");
      return new DecisionResult(saved, updated);
    } catch (java.util.ConcurrentModificationException exception) {
      LOGGER
          .atWarn()
          .addKeyValue("event", "job.strategy.conflict")
          .addKeyValue("jobId", jobId)
          .addKeyValue("commandId", commandId)
          .addKeyValue("reasonCode", "CONCURRENT_MODIFICATION")
          .log("岗位策略决定发生并发冲突");
      throw new JobStrategyException(FailureReason.CONFLICT, "岗位策略已被其他请求修改", exception);
    }
  }

  private static JobStrategy resultingStrategy(DecisionType type) {
    return switch (type) {
      case PROMOTE -> JobStrategy.TARGETED_APPLY;
      case KEEP_BROAD -> JobStrategy.BROAD_APPLY;
      case SKIP -> JobStrategy.SKIPPED;
    };
  }

  public record DecisionResult(JobDecision decision, JobCandidate candidate) {}

  public enum FailureReason {
    NOT_FOUND,
    CONFLICT
  }

  /** 可由 Web Adapter 映射为稳定 HTTP 错误码的领域应用异常。 */
  public static final class JobStrategyException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final FailureReason failureReason;

    public JobStrategyException(FailureReason reason, String message) {
      super(message);
      this.failureReason = reason;
    }

    public JobStrategyException(FailureReason reason, String message, Throwable cause) {
      super(message, cause);
      this.failureReason = reason;
    }

    public FailureReason reason() {
      return failureReason;
    }
  }
}
