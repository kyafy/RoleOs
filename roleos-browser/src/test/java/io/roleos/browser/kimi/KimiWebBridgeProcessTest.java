package io.roleos.browser.kimi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** 真实进程大输出回归：避免快照超过管道容量时导航假超时。 */
class KimiWebBridgeProcessTest {
  @Test
  void refreshesLoadingIframeWithReadonlySnapshot() {
    AtomicInteger calls = new AtomicInteger();
    var provider =
        new KimiWebBridgeBrowserProvider(
            List.of("fixture"),
            Duration.ofSeconds(1),
            Clock.systemUTC(),
            new ObjectMapper(),
            (command, input, timeout) ->
                calls.getAndIncrement() == 0
                    ? "{\"url\":\"https://www.zhipin.com/web/geek/job\",\"accessibilityText\":\"{\\\"tree\\\":[{\\\"role\\\":\\\"Iframe\\\"}]}\"}"
                    : "{\"url\":\"https://www.zhipin.com/web/geek/job\",\"accessibilityText\":\"{\\\"links\\\":[{\\\"url\\\":\\\"https://www.zhipin.com/job_detail/1.html\\\"}]}\"}");
    var context = provider.open(Map.of());
    try {
      var snapshot = provider.navigate(context, URI.create("https://www.zhipin.com/web/geek/job"));
      assertThat(calls).hasValue(2);
      assertThat(snapshot.accessibilityText()).contains("/job_detail/1.html");
    } finally {
      provider.close(context);
    }
  }

  @Test
  void refreshesPendingBossCardsWithoutTreatingThemAsAnEmptyResult() {
    AtomicInteger calls = new AtomicInteger();
    var provider =
        new KimiWebBridgeBrowserProvider(
            List.of("fixture"),
            Duration.ofSeconds(1),
            Clock.systemUTC(),
            new ObjectMapper(),
            (command, input, timeout) ->
                calls.getAndIncrement() == 0
                    ? "{\"url\":\"https://www.zhipin.com/web/geek/job\",\"accessibilityText\":\"{\\\"tree\\\":[],\\\"cards\\\":[]}\"}"
                    : "{\"url\":\"https://www.zhipin.com/web/geek/job\",\"accessibilityText\":\"{\\\"tree\\\":[],\\\"cards\\\":[{\\\"url\\\":\\\"https://www.zhipin.com/job_detail/1.html\\\"}]}\"}");
    var context = provider.open(Map.of());
    try {
      var snapshot = provider.navigate(context, URI.create("https://www.zhipin.com/web/geek/job"));
      assertThat(calls).hasValue(2);
      assertThat(snapshot.accessibilityText()).contains("/job_detail/1.html");
    } finally {
      provider.close(context);
    }
  }

  @Test
  void drainsLargeSnapshotWhileProcessIsRunning() {
    var provider =
        new KimiWebBridgeBrowserProvider(
            List.of(
                "node",
                "-e",
                "process.stdin.resume();process.stdin.on('end',()=>process.stdout.write(JSON.stringify({url:'https://www.zhipin.com/',accessibilityText:'x'.repeat(200000)})));"),
            Duration.ofSeconds(10),
            Clock.systemUTC(),
            new ObjectMapper());
    var context = provider.open(Map.of());
    try {
      var snapshot = provider.navigate(context, URI.create("https://www.zhipin.com/"));
      assertThat(snapshot.accessibilityText()).hasSize(200000);
    } finally {
      provider.close(context);
    }
  }
}
