package io.roleos.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.browser.boss.BossJobSiteAdapter;
import io.roleos.browser.boss.BossSnapshotParser;
import io.roleos.browser.kimi.KimiWebBridgeBrowserProvider;
import io.roleos.browser.playwright.PlaywrightMcpBrowserProvider;
import io.roleos.browser.playwright.PlaywrightMcpBrowserProvider.Config;
import io.roleos.browser.playwright.PlaywrightMcpBrowserProvider.Transport;
import io.roleos.browser.port.BrowserProvider;
import io.roleos.browser.port.BrowserTelemetry;
import io.roleos.browser.router.BrowserRouter;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.port.JobSourcePort;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** 从部署配置装配 Browser 三层边界；禁用 Provider 时仍保留可诊断的 Source 端口。 */
@Configuration(proxyBeanMethods = false)
public class BrowserIntegrationConfiguration {

  @Bean
  public BrowserRouter browserRouter(
      Environment environment, Clock clock, ObjectMapper mapper, BrowserTelemetry telemetry) {
    List<BrowserProvider> providers = new ArrayList<>();
    if (environment.getProperty("roleos.browser.playwright.enabled", Boolean.class, false)) {
      providers.add(playwright(environment, clock));
    }
    if (environment.getProperty("roleos.browser.kimi.enabled", Boolean.class, false)) {
      providers.add(kimi(environment, clock, mapper));
    }
    return new BrowserRouter(
        providers,
        provider(
            environment, "roleos.browser.primary-provider", BrowserProviderType.PLAYWRIGHT_MCP),
        provider(
            environment, "roleos.browser.fallback-provider", BrowserProviderType.KIMI_WEBBRIDGE),
        environment.getProperty("roleos.job.max-step-attempts", Integer.class, 2),
        telemetry);
  }

  @Bean
  public BossSnapshotParser bossSnapshotParser() {
    return new BossSnapshotParser();
  }

  @Bean
  public JobSourcePort bossJobSource(BrowserRouter browserRouter, BossSnapshotParser parser) {
    return new BossJobSiteAdapter(browserRouter, parser);
  }

  @Bean
  public BrowserTelemetry browserTelemetry() {
    return BrowserTelemetry.NOOP;
  }

  private static PlaywrightMcpBrowserProvider playwright(Environment environment, Clock clock) {
    Transport transport =
        Transport.valueOf(environment.getProperty("roleos.browser.playwright.transport", "STDIO"));
    Config config =
        new Config(
            transport,
            environment.getProperty("roleos.browser.playwright.command", "npx"),
            values(
                environment.getProperty(
                    "roleos.browser.playwright.arguments", "@playwright/mcp@latest")),
            URI.create(
                environment.getProperty(
                    "roleos.browser.playwright.endpoint", "http://127.0.0.1:8931/mcp")),
            Duration.parse(
                environment.getProperty("roleos.browser.playwright.request-timeout", "PT30S")));
    return new PlaywrightMcpBrowserProvider(config, clock);
  }

  private static KimiWebBridgeBrowserProvider kimi(
      Environment environment, Clock clock, ObjectMapper mapper) {
    List<String> command = values(environment.getRequiredProperty("roleos.browser.kimi.command"));
    return new KimiWebBridgeBrowserProvider(
        command,
        Duration.parse(environment.getProperty("roleos.browser.kimi.request-timeout", "PT60S")),
        clock,
        mapper);
  }

  private static BrowserProviderType provider(
      Environment environment, String key, BrowserProviderType fallback) {
    return BrowserProviderType.valueOf(environment.getProperty(key, fallback.name()));
  }

  /** 命令与参数使用逗号分隔，避免交给 shell 重新解释。 */
  private static List<String> values(String value) {
    return Arrays.stream(value.split(","))
        .map(String::strip)
        .filter(item -> !item.isEmpty())
        .toList();
  }
}
