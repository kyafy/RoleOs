package io.roleos.boot;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.roleos.browser.port.BrowserTelemetry;
import io.roleos.job.port.JobTelemetry;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Job Intelligence 的 Micrometer 指标适配器；禁止将 searchId、jobId 或原因原文作为 Tag。 */
@Configuration(proxyBeanMethods = false)
public class JobIntelligenceObservabilityConfiguration {

  @Bean
  @Primary
  JobTelemetry micrometerJobTelemetry(MeterRegistry registry) {
    return (searchId, event, status, provider, duration, reasonCode) -> {
      Tags tags =
          Tags.of(
              "event", safe(event),
              "status", safe(status),
              "provider", provider == null ? "unknown" : provider.name().toLowerCase(Locale.ROOT));
      registry.counter("roleos.job.search.total", tags).increment();
      registry.timer("roleos.job.search.phase.duration", tags).record(duration);
      switch (safe(event)) {
        case "failure" -> registry.counter("roleos.job.search.failure.total", tags).increment();
        case "human_pause" ->
            registry.counter("roleos.job.search.human_pause.total", tags).increment();
        default -> {
          // Total and duration metrics already cover all other lifecycle events.
        }
      }
    };
  }

  @Bean
  @Primary
  BrowserTelemetry micrometerBrowserTelemetry(MeterRegistry registry) {
    return (event, provider, targetProvider, duration, reasonCode) -> {
      Tags tags =
          Tags.of(
              "event", safe(event),
              "provider", provider == null ? "unknown" : provider.name().toLowerCase(Locale.ROOT),
              "target_provider",
                  targetProvider == null ? "none" : targetProvider.name().toLowerCase(Locale.ROOT));
      registry.counter("roleos.browser.operation.total", tags).increment();
      registry.timer("roleos.browser.operation.duration", tags).record(duration);
      switch (safe(event)) {
        case "provider_switch" ->
            registry.counter("roleos.browser.provider.switch.total", tags).increment();
        case "failure" -> registry.counter("roleos.browser.failure.total", tags).increment();
        case "human_pause" ->
            registry.counter("roleos.browser.human_pause.total", tags).increment();
        default -> {
          // Total and duration metrics already cover all other lifecycle events.
        }
      }
    };
  }

  private static String safe(String value) {
    return value == null ? "unknown" : value.toLowerCase(Locale.ROOT);
  }
}
