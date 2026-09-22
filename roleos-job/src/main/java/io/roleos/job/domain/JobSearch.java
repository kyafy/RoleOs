package io.roleos.job.domain;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.DecisionType;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.domain.JobTypes.ParseStatus;
import io.roleos.job.domain.JobTypes.SearchState;
import io.roleos.job.domain.JobTypes.SearchStep;
import io.roleos.job.domain.JobTypes.Source;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 可持久化、可恢复的岗位搜索业务状态；Provider 不能直接修改该状态。 */
public record JobSearch(
    UUID id,
    UserId userId,
    Map<String, Object> criteria,
    String startCommandId,
    String resumeCommandId,
    BrowserProviderType requestedProvider,
    BrowserProviderType activeProvider,
    SearchState state,
    SearchStep step,
    URI resumeUrl,
    String cursor,
    String waitReason,
    String failureCode,
    int attemptCount,
    Counts counts,
    Instant startedAt,
    Instant updatedAt,
    Instant completedAt) {

  private static final Map<SearchState, Set<SearchState>> TRANSITIONS =
      Map.of(
          SearchState.CREATED, Set.of(SearchState.RUNNING),
          SearchState.RUNNING,
              Set.of(
                  SearchState.COMPLETED,
                  SearchState.RETRYABLE_FAILED,
                  SearchState.PAUSED_FOR_HUMAN,
                  SearchState.FAILED),
          SearchState.RETRYABLE_FAILED, Set.of(SearchState.RUNNING, SearchState.FAILED),
          SearchState.PAUSED_FOR_HUMAN, Set.of(SearchState.RUNNING, SearchState.FAILED),
          SearchState.FAILED, Set.of(),
          SearchState.COMPLETED, Set.of());

  public JobSearch {
    Objects.requireNonNull(id, "搜索标识不能为空");
    Objects.requireNonNull(userId, "用户不能为空");
    criteria = criteria == null ? Map.of() : Map.copyOf(criteria);
    if (startCommandId != null) requireText(startCommandId, "开始命令标识不能为空");
    if (resumeCommandId != null) requireText(resumeCommandId, "恢复命令标识不能为空");
    Objects.requireNonNull(requestedProvider, "请求 Provider 不能为空");
    Objects.requireNonNull(state, "搜索状态不能为空");
    Objects.requireNonNull(step, "搜索步骤不能为空");
    if (resumeUrl != null && !"https".equalsIgnoreCase(resumeUrl.getScheme())) {
      throw new IllegalArgumentException("恢复链接必须使用 HTTPS");
    }
    if (attemptCount < 0) throw new IllegalArgumentException("重试次数不能为负数");
    counts = counts == null ? Counts.ZERO : counts;
    Objects.requireNonNull(startedAt, "开始时间不能为空");
    Objects.requireNonNull(updatedAt, "更新时间不能为空");
    if (state == SearchState.PAUSED_FOR_HUMAN && isBlank(waitReason)) {
      throw new IllegalArgumentException("人工暂停必须记录等待原因");
    }
    if ((state == SearchState.FAILED || state == SearchState.RETRYABLE_FAILED)
        && isBlank(failureCode)) {
      throw new IllegalArgumentException("失败状态必须记录失败码");
    }
    if (state == SearchState.COMPLETED && completedAt == null) {
      throw new IllegalArgumentException("完成状态必须记录完成时间");
    }
  }

  /**
   * 应用一条由确定性 Workflow Rule 批准的转换。
   *
   * @param target 目标状态
   * @param now 统一时钟产生的事件时间
   * @param userInitiated 仅人工暂停恢复时必须为 true
   */
  public JobSearch transitionTo(SearchState target, Instant now, boolean userInitiated) {
    Objects.requireNonNull(target);
    Objects.requireNonNull(now);
    if (!TRANSITIONS.getOrDefault(state, Set.of()).contains(target)) {
      throw new IllegalStateException("不允许的岗位搜索状态转换: " + state + " -> " + target);
    }
    if (state == SearchState.PAUSED_FOR_HUMAN && target == SearchState.RUNNING && !userInitiated) {
      throw new IllegalStateException("人工暂停只能由用户恢复");
    }
    return new JobSearch(
        id,
        userId,
        criteria,
        startCommandId,
        resumeCommandId,
        requestedProvider,
        activeProvider,
        target,
        step,
        resumeUrl,
        cursor,
        target == SearchState.PAUSED_FOR_HUMAN ? waitReason : null,
        target == SearchState.FAILED || target == SearchState.RETRYABLE_FAILED ? failureCode : null,
        attemptCount,
        counts,
        startedAt,
        now,
        target == SearchState.COMPLETED ? now : completedAt);
  }

  /** 更新可恢复业务步骤；调用方只能写业务 URL/cursor，不能写 Provider 私有状态。 */
  public JobSearch advance(
      SearchStep nextStep,
      BrowserProviderType provider,
      URI nextResumeUrl,
      String nextCursor,
      Instant now) {
    if (state != SearchState.RUNNING) throw new IllegalStateException("只有运行中的搜索可以推进步骤");
    return new JobSearch(
        id,
        userId,
        criteria,
        startCommandId,
        resumeCommandId,
        requestedProvider,
        provider,
        state,
        nextStep,
        nextResumeUrl,
        nextCursor,
        null,
        null,
        attemptCount,
        counts,
        startedAt,
        now,
        null);
  }

  /** 记录人工验证暂停；恢复仍必须通过 transitionTo(..., true)。 */
  public JobSearch pauseForHuman(String reason, Instant now) {
    requireText(reason, "人工暂停原因不能为空");
    if (state != SearchState.RUNNING) throw new IllegalStateException("只有运行中的搜索可以人工暂停");
    return new JobSearch(
        id,
        userId,
        criteria,
        startCommandId,
        resumeCommandId,
        requestedProvider,
        activeProvider,
        SearchState.PAUSED_FOR_HUMAN,
        step,
        resumeUrl,
        cursor,
        reason,
        null,
        attemptCount,
        counts,
        startedAt,
        now,
        null);
  }

  /** 记录可重试或终止失败，并增加尝试次数。 */
  public JobSearch fail(String code, boolean retryable, Instant now) {
    requireText(code, "失败码不能为空");
    if (state != SearchState.RUNNING) throw new IllegalStateException("只有运行中的搜索可以失败");
    return new JobSearch(
        id,
        userId,
        criteria,
        startCommandId,
        resumeCommandId,
        requestedProvider,
        activeProvider,
        retryable ? SearchState.RETRYABLE_FAILED : SearchState.FAILED,
        step,
        resumeUrl,
        cursor,
        null,
        code,
        attemptCount + 1,
        counts,
        startedAt,
        now,
        null);
  }

  /** 搜索事实统计只由确定性编排器更新，供 API 和用户界面观察。 */
  public JobSearch withCounts(Counts nextCounts, Instant now) {
    Objects.requireNonNull(nextCounts);
    Objects.requireNonNull(now);
    return new JobSearch(
        id,
        userId,
        criteria,
        startCommandId,
        resumeCommandId,
        requestedProvider,
        activeProvider,
        state,
        step,
        resumeUrl,
        cursor,
        waitReason,
        failureCode,
        attemptCount,
        nextCounts,
        startedAt,
        now,
        completedAt);
  }

  /** 为人工恢复保存幂等命令；同一命令只可对应同一搜索。 */
  public JobSearch withResumeCommand(String commandId, Instant now) {
    requireText(commandId, "恢复命令标识不能为空");
    return new JobSearch(
        id,
        userId,
        criteria,
        startCommandId,
        commandId,
        requestedProvider,
        activeProvider,
        state,
        step,
        resumeUrl,
        cursor,
        waitReason,
        failureCode,
        attemptCount,
        counts,
        startedAt,
        now,
        completedAt);
  }

  /** 不包含 Provider 内部状态的搜索数量快照。 */
  public record Counts(int discovered, int normalized, int rejected, int ranked) {
    public static final Counts ZERO = new Counts(0, 0, 0, 0);

    public Counts {
      if (discovered < 0 || normalized < 0 || rejected < 0 || ranked < 0) {
        throw new IllegalArgumentException("搜索统计不能为负数");
      }
      if (normalized > discovered || rejected > normalized || ranked > normalized) {
        throw new IllegalArgumentException("搜索统计关系不合法");
      }
    }
  }

  /** 原始来源快照；Provider 私有 Session 与 ElementRef 不得进入此记录。 */
  public record SourceSnapshot(
      UUID id,
      UUID jobId,
      UUID searchId,
      Source source,
      String externalJobId,
      URI sourceUrl,
      String rawPayload,
      String contentHash,
      BrowserProviderType provider,
      Instant fetchedAt,
      ParseStatus parseStatus,
      String failureCode) {
    public SourceSnapshot {
      Objects.requireNonNull(id);
      Objects.requireNonNull(searchId);
      Objects.requireNonNull(source);
      requireText(externalJobId, "外部岗位标识不能为空");
      Objects.requireNonNull(sourceUrl);
      requireText(rawPayload, "原始来源数据不能为空");
      if (contentHash == null || !contentHash.matches("[a-f0-9]{64}")) {
        throw new IllegalArgumentException("快照摘要必须是小写 SHA-256");
      }
      Objects.requireNonNull(provider);
      Objects.requireNonNull(fetchedAt);
      Objects.requireNonNull(parseStatus);
      if (parseStatus == ParseStatus.FAILED && isBlank(failureCode)) {
        throw new IllegalArgumentException("解析失败必须记录失败码");
      }
    }
  }

  /** 用户岗位策略决策的不可变审计记录。 */
  public record JobDecision(
      UUID id,
      UUID candidateId,
      UserId userId,
      String commandId,
      DecisionType decision,
      JobStrategy previousStrategy,
      JobStrategy resultingStrategy,
      String reason,
      Instant decidedAt) {
    public JobDecision {
      Objects.requireNonNull(id);
      Objects.requireNonNull(candidateId);
      Objects.requireNonNull(userId);
      requireText(commandId, "幂等命令标识不能为空");
      Objects.requireNonNull(decision);
      Objects.requireNonNull(previousStrategy);
      Objects.requireNonNull(resultingStrategy);
      Objects.requireNonNull(decidedAt);
    }
  }

  private static void requireText(String value, String message) {
    if (isBlank(value)) throw new IllegalArgumentException(message);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
