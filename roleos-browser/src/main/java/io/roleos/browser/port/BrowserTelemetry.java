package io.roleos.browser.port;

import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.time.Duration;

/** Browser Provider 的低基数指标端口。 */
@FunctionalInterface
public interface BrowserTelemetry {

  BrowserTelemetry NOOP = (event, provider, targetProvider, duration, reasonCode) -> {};

  void record(
      String event,
      BrowserProviderType provider,
      BrowserProviderType targetProvider,
      Duration duration,
      String reasonCode);
}
