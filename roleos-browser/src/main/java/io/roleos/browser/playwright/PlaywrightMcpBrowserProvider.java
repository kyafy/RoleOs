package io.roleos.browser.playwright;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.roleos.browser.port.AbstractBrowserProvider;
import io.roleos.browser.port.BrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.browser.port.BrowserProvider.PageSnapshot;
import io.roleos.browser.port.BrowserProvider.ProviderContext;
import io.roleos.browser.port.BrowserProvider.ProviderElementRef;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 通过官方 MCP Java SDK 调用 Playwright MCP 的受限 Browser Provider。 */
public final class PlaywrightMcpBrowserProvider extends AbstractBrowserProvider {

  private static final Pattern REF_PATTERN = Pattern.compile("\\bref[=:]([A-Za-z0-9_-]+)");

  private final Config config;
  private final Clock clock;
  private final McpSessionFactory sessions;
  private final Map<UUID, McpSession> active = new ConcurrentHashMap<>();
  private final Map<UUID, URI> currentUrls = new ConcurrentHashMap<>();

  public PlaywrightMcpBrowserProvider(Config config, Clock clock) {
    this(config, clock, PlaywrightMcpBrowserProvider::openSdkSession);
  }

  PlaywrightMcpBrowserProvider(Config config, Clock clock, McpSessionFactory sessions) {
    this.config = config;
    this.clock = clock;
    this.sessions = sessions;
  }

  @Override
  public BrowserProviderType type() {
    return BrowserProviderType.PLAYWRIGHT_MCP;
  }

  @Override
  public ProviderContext open(Map<String, String> safeConfiguration) {
    try {
      ProviderContext context = new ProviderContext(UUID.randomUUID(), type());
      active.put(context.providerInstanceId(), sessions.open(config));
      return context;
    } catch (RuntimeException exception) {
      throw switched("MCP_INITIALIZATION_FAILED", exception);
    }
  }

  @Override
  public PageSnapshot navigate(ProviderContext context, URI url) {
    requireUsable(context);
    BrowserProvider.requireTrustedUrl(url);
    call(context, "browser_navigate", Map.of("url", url.toString()));
    currentUrls.put(context.providerInstanceId(), url);
    return snapshot(context);
  }

  @Override
  public PageSnapshot snapshot(ProviderContext context) {
    requireUsable(context);
    String text = call(context, "browser_snapshot", Map.of());
    detectHumanVerification(text);
    URI url = currentUrls.get(context.providerInstanceId());
    if (url == null) throw switched("MCP_PAGE_NOT_NAVIGATED");
    return page(context, url, text);
  }

  @Override
  public PageSnapshot click(ProviderContext context, ProviderElementRef element) {
    requireOwned(context, element);
    call(context, "browser_click", Map.of("ref", element.reference()));
    return snapshot(context);
  }

  @Override
  public PageSnapshot fill(ProviderContext context, ProviderElementRef element, String value) {
    requireOwned(context, element);
    call(context, "browser_type", Map.of("ref", element.reference(), "text", value));
    return snapshot(context);
  }

  @Override
  protected void closeInternal(ProviderContext context) {
    McpSession session = active.remove(context.providerInstanceId());
    currentUrls.remove(context.providerInstanceId());
    if (session != null) session.close();
  }

  private String call(ProviderContext context, String tool, Map<String, Object> arguments) {
    try {
      return active.get(context.providerInstanceId()).call(tool, arguments);
    } catch (BrowserProviderException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw switched("MCP_TOOL_FAILED", exception);
    }
  }

  private PageSnapshot page(ProviderContext context, URI url, String text) {
    List<ProviderElementRef> refs = new ArrayList<>();
    Matcher matcher = REF_PATTERN.matcher(text);
    while (matcher.find()) {
      refs.add(new ProviderElementRef(context.providerInstanceId(), matcher.group(1)));
    }
    return new PageSnapshot(context.providerInstanceId(), url, text, refs, clock.instant());
  }

  private static void detectHumanVerification(String text) {
    String lower = text.toLowerCase(java.util.Locale.ROOT);
    if (lower.contains("captcha") || text.contains("验证码") || text.contains("安全验证")) {
      throw new BrowserProviderException(
          FailureCategory.PAUSE_FOR_HUMAN, "HUMAN_VERIFICATION_REQUIRED", "页面要求用户完成人工验证");
    }
  }

  private static BrowserProviderException switched(String code) {
    return new BrowserProviderException(
        FailureCategory.SWITCH_PROVIDER, code, "Playwright MCP 暂不可用");
  }

  private static BrowserProviderException switched(String code, RuntimeException cause) {
    return new BrowserProviderException(
        FailureCategory.SWITCH_PROVIDER, code, "Playwright MCP 暂不可用", cause);
  }

  @SuppressWarnings("PMD.CloseResource") // Ownership is transferred to the returned McpSession.
  private static McpSession openSdkSession(Config config) {
    var mapper = new JacksonMcpJsonMapper(new ObjectMapper());
    McpClientTransport transport;
    if (config.transport() == Transport.STDIO) {
      var parameters = ServerParameters.builder(config.command()).args(config.arguments()).build();
      transport = new StdioClientTransport(parameters, mapper);
    } else {
      transport =
          HttpClientStreamableHttpTransport.builder(config.endpoint().toString())
              .jsonMapper(mapper)
              .connectTimeout(config.timeout())
              .build();
    }
    McpSyncClient client =
        McpClient.sync(transport)
            .requestTimeout(config.timeout())
            .initializationTimeout(config.timeout())
            .build();
    client.initialize();
    var tools =
        client.listTools().tools().stream()
            .map(McpSchema.Tool::name)
            .collect(java.util.stream.Collectors.toSet());
    if (!tools.containsAll(List.of("browser_navigate", "browser_snapshot"))) {
      client.close();
      throw new IllegalStateException("Playwright MCP 缺少必要工具");
    }
    return new McpSession() {
      @Override
      public String call(String tool, Map<String, Object> arguments) {
        var result = client.callTool(new McpSchema.CallToolRequest(tool, arguments));
        if (Boolean.TRUE.equals(result.isError()))
          throw new IllegalStateException("MCP tool error");
        return result.content().stream()
            .filter(McpSchema.TextContent.class::isInstance)
            .map(McpSchema.TextContent.class::cast)
            .map(McpSchema.TextContent::text)
            .collect(java.util.stream.Collectors.joining("\n"));
      }

      @Override
      public void close() {
        client.closeGracefully();
      }
    };
  }

  public enum Transport {
    STDIO,
    HTTP
  }

  public record Config(
      Transport transport, String command, List<String> arguments, URI endpoint, Duration timeout) {
    public Config {
      if (transport == null) transport = Transport.STDIO;
      arguments = arguments == null ? List.of() : List.copyOf(arguments);
      if (timeout == null || timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("MCP 超时必须为正数");
      }
      if (transport == Transport.STDIO && (command == null || command.isBlank())) {
        throw new IllegalArgumentException("STDIO command 不能为空");
      }
      if (transport == Transport.HTTP && endpoint == null) {
        throw new IllegalArgumentException("HTTP endpoint 不能为空");
      }
    }
  }

  @FunctionalInterface
  interface McpSessionFactory {
    McpSession open(Config config);
  }

  interface McpSession {
    String call(String tool, Map<String, Object> arguments);

    void close();
  }
}
