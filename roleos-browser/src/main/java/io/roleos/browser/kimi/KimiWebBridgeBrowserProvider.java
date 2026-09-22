package io.roleos.browser.kimi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.browser.port.AbstractBrowserProvider;
import io.roleos.browser.port.BrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.browser.port.BrowserProvider.PageSnapshot;
import io.roleos.browser.port.BrowserProvider.ProviderContext;
import io.roleos.browser.port.BrowserProvider.ProviderElementRef;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 通过部署配置的本地 Kimi Agent 命令交换严格 JSON，不读取扩展内部状态。 */
public final class KimiWebBridgeBrowserProvider extends AbstractBrowserProvider {

  private static final int MAX_RESPONSE_BYTES = 2_097_152;
  private static final int PROCESS_SUCCESS = 0;

  private final List<String> command;
  private final Duration timeout;
  private final Clock clock;
  private final ObjectMapper mapper;
  private final CommandExecutor executor;
  private final Map<UUID, URI> currentUrls = new ConcurrentHashMap<>();
  private final Map<UUID, String> sessions = new ConcurrentHashMap<>();

  public KimiWebBridgeBrowserProvider(
      List<String> command, Duration timeout, Clock clock, ObjectMapper mapper) {
    this(command, timeout, clock, mapper, KimiWebBridgeBrowserProvider::executeProcess);
  }

  KimiWebBridgeBrowserProvider(
      List<String> command,
      Duration timeout,
      Clock clock,
      ObjectMapper mapper,
      CommandExecutor executor) {
    if (command == null || command.isEmpty() || command.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException("Kimi 本地 Agent 命令不能为空");
    }
    this.command = List.copyOf(command);
    this.timeout = timeout;
    this.clock = clock;
    this.mapper = mapper;
    this.executor = executor;
  }

  @Override
  public BrowserProviderType type() {
    return BrowserProviderType.KIMI_WEBBRIDGE;
  }

  @Override
  public ProviderContext open(Map<String, String> safeConfiguration) {
    ProviderContext context = new ProviderContext(UUID.randomUUID(), type());
    String searchId =
        safeConfiguration.getOrDefault("searchId", context.providerInstanceId().toString());
    sessions.put(context.providerInstanceId(), "roleos-" + UUID.fromString(searchId));
    return context;
  }

  @Override
  public PageSnapshot navigate(ProviderContext context, URI url) {
    requireUsable(context);
    BrowserProvider.requireTrustedUrl(url);
    currentUrls.put(context.providerInstanceId(), url);
    PageSnapshot snapshot = invoke(context, "navigate", Map.of("url", url.toString()));
    // Boss 搜索页的卡片也可能在顶层页面完成后异步渲染。仅做有界只读刷新：真正空结果
    // 最终仍交由站点适配器安全失败，不能以等待或重试猜测岗位字段。
    for (int attempt = 0;
        attempt < 3 && requiresSnapshotRefresh(snapshot.accessibilityText());
        attempt++) {
      waitForPageSettlement();
      snapshot = invoke(context, "snapshot", Map.of());
    }
    return snapshot;
  }

  @Override
  public PageSnapshot snapshot(ProviderContext context) {
    return invoke(context, "snapshot", Map.of());
  }

  @Override
  public PageSnapshot click(ProviderContext context, ProviderElementRef element) {
    requireOwned(context, element);
    return invoke(context, "click", Map.of("ref", element.reference()));
  }

  @Override
  public PageSnapshot fill(ProviderContext context, ProviderElementRef element, String value) {
    requireOwned(context, element);
    return invoke(context, "fill", Map.of("ref", element.reference(), "value", value));
  }

  @Override
  protected void closeInternal(ProviderContext context) {
    currentUrls.remove(context.providerInstanceId());
    sessions.remove(context.providerInstanceId());
  }

  private PageSnapshot invoke(
      ProviderContext context, String operation, Map<String, String> arguments) {
    requireUsable(context);
    URI current = currentUrls.get(context.providerInstanceId());
    if (current == null && !"navigate".equals(operation)) throw switched("KIMI_PAGE_NOT_NAVIGATED");
    try {
      String input =
          mapper.writeValueAsString(
              Map.of(
                  "operation",
                  operation,
                  "arguments",
                  arguments,
                  "session",
                  sessions.get(context.providerInstanceId())));
      JsonNode root = mapper.readTree(executor.execute(command, input, timeout));
      if (!root.isObject() || !root.path("accessibilityText").isTextual()) {
        throw switched("KIMI_INVALID_RESPONSE");
      }
      String text = root.path("accessibilityText").textValue();
      if (root.path("humanVerificationRequired").asBoolean(false)) {
        throw new BrowserProviderException(
            FailureCategory.PAUSE_FOR_HUMAN, "HUMAN_VERIFICATION_REQUIRED", "页面要求用户完成人工验证");
      }
      List<ProviderElementRef> refs = new ArrayList<>();
      root.path("elementRefs")
          .forEach(
              value ->
                  refs.add(new ProviderElementRef(context.providerInstanceId(), value.asText())));
      URI url =
          URI.create(
              root.path("url").asText(currentUrls.get(context.providerInstanceId()).toString()));
      BrowserProvider.requireTrustedUrl(url);
      currentUrls.put(context.providerInstanceId(), url);
      return new PageSnapshot(context.providerInstanceId(), url, text, refs, clock.instant());
    } catch (BrowserProviderException exception) {
      throw exception;
    } catch (JsonProcessingException exception) {
      throw switched("KIMI_INVALID_JSON", exception);
    }
  }

  private static String executeProcess(List<String> command, String input, Duration timeout) {
    Process process = null;
    try {
      process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
      try (var stdout = process.getInputStream()) {
        // 同时读取输出，避免真实大快照填满 OS pipe 后与 waitFor 相互等待。
        FutureTask<byte[]> output =
            new FutureTask<>(() -> stdout.readNBytes(MAX_RESPONSE_BYTES + 1));
        Thread.ofVirtual().start(output);
        process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().close();
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          throw switched("KIMI_TIMEOUT");
        }
        if (process.exitValue() != PROCESS_SUCCESS) throw switched("KIMI_PROCESS_FAILED");
        byte[] bytes = output.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (bytes.length > MAX_RESPONSE_BYTES) throw switched("KIMI_RESPONSE_TOO_LARGE");
        return new String(bytes, StandardCharsets.UTF_8);
      }
    } catch (IOException | ExecutionException | TimeoutException exception) {
      throw switched("KIMI_PROCESS_FAILED", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw switched("KIMI_INTERRUPTED", exception);
    } finally {
      if (process != null && process.isAlive()) process.destroyForcibly();
    }
  }

  private static BrowserProviderException switched(String code) {
    return new BrowserProviderException(FailureCategory.SWITCH_PROVIDER, code, "Kimi 浏览能力暂不可用");
  }

  private static BrowserProviderException switched(String code, Exception cause) {
    return new BrowserProviderException(
        FailureCategory.SWITCH_PROVIDER, code, "Kimi 浏览能力暂不可用", cause);
  }

  private static boolean requiresSnapshotRefresh(String accessibilityText) {
    String text = accessibilityText == null ? "" : accessibilityText;
    boolean iframeShell =
        (text.contains("\"role\":\"Iframe\"") || text.contains("\"role\":\"iframe\""))
            && !text.contains("/job_detail/");
    return iframeShell || text.contains("\"cards\":[]");
  }

  private static void waitForPageSettlement() {
    try {
      Thread.sleep(250);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw switched("KIMI_INTERRUPTED", exception);
    }
  }

  @FunctionalInterface
  interface CommandExecutor {
    String execute(List<String> command, String input, Duration timeout);
  }
}
