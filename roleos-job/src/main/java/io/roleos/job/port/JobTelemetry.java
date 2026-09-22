package io.roleos.job.port;

import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.time.Duration;
import java.util.UUID;

/** Job Intelligence 的低基数可观测性端口；实现不得记录 JD、页面内容或凭据。 */
@FunctionalInterface
public interface JobTelemetry {

  JobTelemetry NOOP = (searchId, event, status, provider, duration, reasonCode) -> {};

  void record(
      UUID searchId,
      String event,
      String status,
      BrowserProviderType provider,
      Duration duration,
      String reasonCode);
}
