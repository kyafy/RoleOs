package io.roleos.browser.port;

import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** 浏览能力的最小安全边界；所有临时引用都绑定单一 Provider 实例。 */
public interface BrowserProvider {

  BrowserProviderType type();

  ProviderContext open(Map<String, String> safeConfiguration);

  PageSnapshot navigate(ProviderContext context, URI url);

  PageSnapshot snapshot(ProviderContext context);

  PageSnapshot click(ProviderContext context, ProviderElementRef element);

  PageSnapshot fill(ProviderContext context, ProviderElementRef element, String value);

  void close(ProviderContext context);

  /** 只接受 HTTPS Boss 链接，避免任意 URL 与本地资源访问。 */
  static void requireTrustedUrl(URI url) {
    Objects.requireNonNull(url, "导航地址不能为空");
    String host = url.getHost();
    if (!"https".equalsIgnoreCase(url.getScheme())
        || host == null
        || !("zhipin.com".equals(host) || host.endsWith(".zhipin.com"))) {
      throw new BrowserProviderException(
          FailureCategory.TERMINAL_FAILURE, "UNTRUSTED_URL", "仅允许受信的 Boss HTTPS 地址");
    }
  }

  /** Provider 实例上下文；closed 后所有操作必须失败。 */
  final class ProviderContext {
    private final UUID instanceId;
    private final BrowserProviderType type;
    private final AtomicBoolean closedState = new AtomicBoolean();

    public ProviderContext(UUID providerInstanceId, BrowserProviderType providerType) {
      this.instanceId = Objects.requireNonNull(providerInstanceId);
      this.type = Objects.requireNonNull(providerType);
    }

    public UUID providerInstanceId() {
      return instanceId;
    }

    public BrowserProviderType providerType() {
      return type;
    }

    public boolean closed() {
      return closedState.get();
    }

    public void markClosed() {
      closedState.set(true);
    }
  }

  record ProviderElementRef(UUID providerInstanceId, String reference) {
    public ProviderElementRef {
      Objects.requireNonNull(providerInstanceId);
      if (reference == null || reference.isBlank()) {
        throw new IllegalArgumentException("元素引用不能为空");
      }
    }
  }

  record PageSnapshot(
      UUID providerInstanceId,
      URI url,
      String accessibilityText,
      List<ProviderElementRef> elements,
      Instant capturedAt) {
    public PageSnapshot {
      Objects.requireNonNull(providerInstanceId);
      requireTrustedUrl(url);
      if (accessibilityText == null) accessibilityText = "";
      elements = elements == null ? List.of() : List.copyOf(elements);
      if (elements.stream()
          .anyMatch(value -> !value.providerInstanceId().equals(providerInstanceId))) {
        throw new BrowserProviderException(
            FailureCategory.TERMINAL_FAILURE, "CROSS_PROVIDER_REFERENCE", "快照包含其他 Provider 的元素引用");
      }
      Objects.requireNonNull(capturedAt);
    }
  }

  enum FailureCategory {
    RETRY_SAME_PROVIDER,
    SWITCH_PROVIDER,
    PAUSE_FOR_HUMAN,
    TERMINAL_FAILURE
  }

  /** 可由 Router 确定性分类的安全异常，不携带页面原文或凭据。 */
  final class BrowserProviderException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final FailureCategory failureCategory;
    private final String safeReasonCode;

    public BrowserProviderException(
        FailureCategory category, String reasonCode, String safeMessage) {
      super(safeMessage);
      this.failureCategory = Objects.requireNonNull(category);
      this.safeReasonCode = Objects.requireNonNull(reasonCode);
    }

    public BrowserProviderException(
        FailureCategory category, String reasonCode, String safeMessage, Throwable cause) {
      super(safeMessage, cause);
      this.failureCategory = Objects.requireNonNull(category);
      this.safeReasonCode = Objects.requireNonNull(reasonCode);
    }

    public FailureCategory category() {
      return failureCategory;
    }

    public String reasonCode() {
      return safeReasonCode;
    }
  }
}
