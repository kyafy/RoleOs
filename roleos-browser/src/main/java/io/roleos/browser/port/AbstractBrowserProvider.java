package io.roleos.browser.port;

import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.browser.port.BrowserProvider.ProviderContext;
import io.roleos.browser.port.BrowserProvider.ProviderElementRef;

/** 统一实现关闭状态、Provider 类型和跨实例引用校验。 */
public abstract class AbstractBrowserProvider implements BrowserProvider {

  protected final void requireUsable(ProviderContext context) {
    if (context.providerType() != type()) {
      throw failure("CROSS_PROVIDER_CONTEXT");
    }
    if (context.closed()) {
      throw failure("PROVIDER_CONTEXT_CLOSED");
    }
  }

  protected final void requireOwned(ProviderContext context, ProviderElementRef element) {
    requireUsable(context);
    if (!context.providerInstanceId().equals(element.providerInstanceId())) {
      throw failure("CROSS_PROVIDER_REFERENCE");
    }
  }

  @Override
  public void close(ProviderContext context) {
    if (context.providerType() != type()) {
      throw failure("CROSS_PROVIDER_CONTEXT");
    }
    if (!context.closed()) {
      closeInternal(context);
      context.markClosed();
    }
  }

  protected abstract void closeInternal(ProviderContext context);

  private BrowserProviderException failure(String reasonCode) {
    return new BrowserProviderException(
        FailureCategory.TERMINAL_FAILURE, reasonCode, "浏览上下文或引用不属于当前 Provider");
  }
}
