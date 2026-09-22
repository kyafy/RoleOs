package io.roleos.browser;

import io.roleos.browser.port.AbstractBrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.browser.port.BrowserProvider.PageSnapshot;
import io.roleos.browser.port.BrowserProvider.ProviderContext;
import io.roleos.browser.port.BrowserProvider.ProviderElementRef;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 可编排调用顺序、成功、失败和人工验证的 Browser Provider Fixture。 */
public final class FakeBrowserProvider extends AbstractBrowserProvider {

  private final BrowserProviderType type;
  private final Deque<RuntimeException> failures = new ArrayDeque<>();
  private final Deque<RuntimeException> openFailures = new ArrayDeque<>();
  private final List<String> calls = new ArrayList<>();
  private URI currentUrl = URI.create("https://www.zhipin.com/");
  private String content = "page fixture";

  public FakeBrowserProvider(BrowserProviderType type) {
    this.type = type;
  }

  public FakeBrowserProvider failNext(FailureCategory category, String code) {
    failures.add(new BrowserProviderException(category, code, "fixture failure"));
    return this;
  }

  /** 模拟 Provider 会话尚未建立时的故障，验证 Router 不会把初始化失败误判为终态。 */
  public FakeBrowserProvider failOpenNext(FailureCategory category, String code) {
    openFailures.add(new BrowserProviderException(category, code, "fixture open failure"));
    return this;
  }

  public FakeBrowserProvider content(String value) {
    content = value;
    return this;
  }

  public List<String> calls() {
    return List.copyOf(calls);
  }

  @Override
  public BrowserProviderType type() {
    return type;
  }

  @Override
  public ProviderContext open(Map<String, String> safeConfiguration) {
    calls.add("open");
    if (!openFailures.isEmpty()) throw openFailures.removeFirst();
    return new ProviderContext(UUID.randomUUID(), type);
  }

  @Override
  public PageSnapshot navigate(ProviderContext context, URI url) {
    requireUsable(context);
    io.roleos.browser.port.BrowserProvider.requireTrustedUrl(url);
    calls.add("navigate");
    failIfPlanned();
    currentUrl = url;
    return page(context);
  }

  @Override
  public PageSnapshot snapshot(ProviderContext context) {
    requireUsable(context);
    calls.add("snapshot");
    failIfPlanned();
    return page(context);
  }

  @Override
  public PageSnapshot click(ProviderContext context, ProviderElementRef element) {
    requireOwned(context, element);
    calls.add("click");
    failIfPlanned();
    return page(context);
  }

  @Override
  public PageSnapshot fill(ProviderContext context, ProviderElementRef element, String value) {
    requireOwned(context, element);
    calls.add("fill");
    failIfPlanned();
    return page(context);
  }

  @Override
  protected void closeInternal(ProviderContext context) {
    calls.add("close");
  }

  private PageSnapshot page(ProviderContext context) {
    return new PageSnapshot(
        context.providerInstanceId(),
        currentUrl,
        content,
        List.of(new ProviderElementRef(context.providerInstanceId(), "fixture-ref")),
        Instant.now());
  }

  private void failIfPlanned() {
    if (!failures.isEmpty()) throw failures.removeFirst();
  }
}
