package io.roleos.browser.router;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.roleos.browser.FakeBrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.browser.router.BrowserRouter.BrowserTaskState;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import io.roleos.job.domain.JobTypes.SearchStep;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Router 只能从业务状态恢复，切换后必须重新 open + navigate。 */
class BrowserRouterRecoveryTest {

  @Test
  void retriesSameProviderThenSucceeds() {
    var primary =
        new FakeBrowserProvider(BrowserProviderType.PLAYWRIGHT_MCP)
            .failNext(FailureCategory.RETRY_SAME_PROVIDER, "TIMEOUT");
    var router = router(primary, new FakeBrowserProvider(BrowserProviderType.KIMI_WEBBRIDGE));

    var result = router.navigate(state(), BrowserProviderType.AUTO);

    assertThat(result.provider()).isEqualTo(BrowserProviderType.PLAYWRIGHT_MCP);
    assertThat(primary.calls()).containsExactly("open", "navigate", "navigate");
  }

  @Test
  void switchClosesOldContextAndFallbackStartsWithNavigate() {
    var primary =
        new FakeBrowserProvider(BrowserProviderType.PLAYWRIGHT_MCP)
            .failNext(FailureCategory.SWITCH_PROVIDER, "MCP_EXITED");
    var fallback = new FakeBrowserProvider(BrowserProviderType.KIMI_WEBBRIDGE);

    var result = router(primary, fallback).navigate(state(), BrowserProviderType.AUTO);

    assertThat(result.provider()).isEqualTo(BrowserProviderType.KIMI_WEBBRIDGE);
    assertThat(primary.calls()).containsExactly("open", "navigate", "close");
    assertThat(fallback.calls()).containsExactly("open", "navigate");
  }

  @Test
  void switchesWhenPrimaryProviderCannotInitialize() {
    var primary =
        new FakeBrowserProvider(BrowserProviderType.PLAYWRIGHT_MCP)
            .failOpenNext(FailureCategory.SWITCH_PROVIDER, "MCP_INITIALIZATION_FAILED");
    var fallback = new FakeBrowserProvider(BrowserProviderType.KIMI_WEBBRIDGE);

    var result = router(primary, fallback).navigate(state(), BrowserProviderType.AUTO);

    assertThat(result.provider()).isEqualTo(BrowserProviderType.KIMI_WEBBRIDGE);
    assertThat(primary.calls()).containsExactly("open");
    assertThat(fallback.calls()).containsExactly("open", "navigate");
  }

  @Test
  void humanAndTerminalFailuresAreNeverRetriedOrSwitched() {
    for (FailureCategory category :
        List.of(FailureCategory.PAUSE_FOR_HUMAN, FailureCategory.TERMINAL_FAILURE)) {
      var primary =
          new FakeBrowserProvider(BrowserProviderType.PLAYWRIGHT_MCP)
              .failNext(category, category.name());
      assertThatThrownBy(
              () ->
                  router(primary, new FakeBrowserProvider(BrowserProviderType.KIMI_WEBBRIDGE))
                      .navigate(state(), BrowserProviderType.AUTO))
          .isInstanceOfSatisfying(
              BrowserProviderException.class,
              exception -> assertThat(exception.category()).isEqualTo(category));
      assertThat(primary.calls()).containsExactly("open", "navigate", "close");
    }
  }

  private BrowserRouter router(FakeBrowserProvider primary, FakeBrowserProvider fallback) {
    return new BrowserRouter(
        List.of(primary, fallback),
        BrowserProviderType.PLAYWRIGHT_MCP,
        BrowserProviderType.KIMI_WEBBRIDGE,
        2);
  }

  private BrowserTaskState state() {
    return new BrowserTaskState(
        UUID.randomUUID(),
        SearchStep.LIST_SNAPSHOT,
        URI.create("https://www.zhipin.com/web/geek/job"),
        "page=1");
  }
}
