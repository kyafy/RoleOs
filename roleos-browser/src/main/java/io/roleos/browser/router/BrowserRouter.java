package io.roleos.browser.router;

import io.roleos.browser.port.BrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.PageSnapshot;
import io.roleos.browser.port.BrowserProvider.ProviderContext;
import io.roleos.browser.port.BrowserTelemetry;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.SearchStep;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 按确定性错误分类执行有界重试、Provider 切换或人工暂停。 */
public final class BrowserRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger(BrowserRouter.class);

  private final Map<BrowserProviderType, BrowserProvider> providers;
  private final BrowserProviderType primary;
  private final BrowserProviderType fallback;
  private final int maxAttempts;
  private final BrowserTelemetry telemetry;

  public BrowserRouter(
      List<BrowserProvider> providers,
      BrowserProviderType primary,
      BrowserProviderType fallback,
      int maxAttempts) {
    this(providers, primary, fallback, maxAttempts, BrowserTelemetry.NOOP);
  }

  public BrowserRouter(
      List<BrowserProvider> providers,
      BrowserProviderType primary,
      BrowserProviderType fallback,
      int maxAttempts,
      BrowserTelemetry telemetry) {
    this.providers =
        providers.stream()
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                    BrowserProvider::type, value -> value));
    this.primary = Objects.requireNonNull(primary);
    this.fallback = Objects.requireNonNull(fallback);
    if (maxAttempts < 1 || maxAttempts > 5) throw new IllegalArgumentException("重试次数必须在 1..5");
    this.maxAttempts = maxAttempts;
    this.telemetry = Objects.requireNonNull(telemetry);
  }

  /** 每次执行都从持久化业务 URL 重新导航，绝不接收 Provider 私有句柄。 */
  public RoutedSnapshot navigate(BrowserTaskState state, BrowserProviderType requested) {
    Instant started = Instant.now();
    BrowserProviderType selected = requested == BrowserProviderType.AUTO ? primary : requested;
    BrowserProvider provider = requireProvider(selected);
    Optional<ProviderContext> context = Optional.empty();
    int attempts = 0;
    while (true) {
      try {
        // Provider 初始化也属于可恢复边界；若 MCP 进程无法建立，必须与 navigate 失败一样切换，
        // 且备用 Provider 只能从持久化业务 URL 开始，不能复用任何旧会话状态。
        if (context.isEmpty()) {
          context = Optional.of(provider.open(Map.of("searchId", state.searchId().toString())));
        }
        attempts++;
        PageSnapshot page = provider.navigate(context.orElseThrow(), state.resumeUrl());
        Duration duration = Duration.between(started, Instant.now());
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(
              "event=browser.navigate.completed searchId={} provider={} durationMs={} status=SUCCESS",
              state.searchId(),
              provider.type(),
              duration.toMillis());
        }
        telemetry.record("navigate", provider.type(), null, duration, null);
        return new RoutedSnapshot(provider.type(), page, attempts);
      } catch (BrowserProviderException exception) {
        if (exception.category() == BrowserProvider.FailureCategory.RETRY_SAME_PROVIDER
            && attempts < maxAttempts) {
          continue;
        }
        if (exception.category() == BrowserProvider.FailureCategory.SWITCH_PROVIDER
            && provider.type() != fallback) {
          BrowserProviderType previous = provider.type();
          context.ifPresent(provider::close);
          provider = requireProvider(fallback);
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(
                "event=browser.provider.switched searchId={} provider={} targetProvider={} status=RECOVERING reasonCode={}",
                state.searchId(),
                previous,
                provider.type(),
                exception.reasonCode());
          }
          telemetry.record(
              "provider_switch",
              previous,
              provider.type(),
              Duration.between(started, Instant.now()),
              exception.reasonCode());
          context = Optional.empty();
          attempts = 0;
          continue;
        }
        context.ifPresent(provider::close);
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(
              "event=browser.navigate.failed searchId={} provider={} durationMs={} status={} reasonCode={}",
              state.searchId(),
              provider.type(),
              Duration.between(started, Instant.now()).toMillis(),
              exception.category(),
              exception.reasonCode());
        }
        telemetry.record(
            exception.category() == BrowserProvider.FailureCategory.PAUSE_FOR_HUMAN
                ? "human_pause"
                : "failure",
            provider.type(),
            null,
            Duration.between(started, Instant.now()),
            exception.reasonCode());
        throw exception;
      }
    }
  }

  private BrowserProvider requireProvider(BrowserProviderType type) {
    BrowserProvider provider = providers.get(type);
    if (provider == null) {
      throw new BrowserProviderException(
          BrowserProvider.FailureCategory.TERMINAL_FAILURE, "PROVIDER_UNAVAILABLE", "请求的浏览能力不可用");
    }
    return provider;
  }

  public record BrowserTaskState(UUID searchId, SearchStep step, URI resumeUrl, String cursor) {
    public BrowserTaskState {
      Objects.requireNonNull(searchId);
      Objects.requireNonNull(step);
      BrowserProvider.requireTrustedUrl(resumeUrl);
    }
  }

  public record RoutedSnapshot(
      BrowserProviderType provider, PageSnapshot snapshot, int attemptCount) {}
}
