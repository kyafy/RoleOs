package io.roleos.boot;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 指标适配器只暴露低基数状态，不使用业务标识或原因文本作为 Tag。 */
class JobIntelligenceObservabilityConfigurationTest {

  @Test
  void recordsSearchFailurePauseAndDuration() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    var telemetry =
        new JobIntelligenceObservabilityConfiguration().micrometerJobTelemetry(registry);

    telemetry.record(
        UUID.randomUUID(),
        "failure",
        "RETRYABLE_FAILED",
        BrowserProviderType.PLAYWRIGHT_MCP,
        Duration.ofMillis(25),
        "secret-reason-must-not-be-tagged");
    telemetry.record(
        UUID.randomUUID(),
        "human_pause",
        "PAUSED_FOR_HUMAN",
        BrowserProviderType.KIMI_WEBBRIDGE,
        Duration.ofMillis(30),
        "CAPTCHA");

    assertThat(registry.find("roleos.job.search.failure.total").counter().count()).isEqualTo(1);
    assertThat(registry.find("roleos.job.search.human_pause.total").counter().count()).isEqualTo(1);
    assertThat(registry.getMeters().stream().flatMap(meter -> meter.getId().getTags().stream()))
        .noneMatch(tag -> tag.getValue().contains("secret"));
  }

  @Test
  void recordsProviderSwitch() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    var telemetry =
        new JobIntelligenceObservabilityConfiguration().micrometerBrowserTelemetry(registry);

    telemetry.record(
        "provider_switch",
        BrowserProviderType.PLAYWRIGHT_MCP,
        BrowserProviderType.KIMI_WEBBRIDGE,
        Duration.ofMillis(12),
        "MCP_TOOL_FAILED");

    assertThat(registry.find("roleos.browser.provider.switch.total").counter().count())
        .isEqualTo(1);
  }
}
