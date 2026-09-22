package io.roleos.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.ProviderElementRef;
import io.roleos.job.domain.JobTypes.BrowserProviderType;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 所有 Provider 必须共享 URL、实例隔离、关闭幂等与人工验证合同。 */
class BrowserProviderContractTest {

  @ParameterizedTest
  @MethodSource("providers")
  void enforcesSecurityAndLifecycle(FakeBrowserProvider provider) {
    var context = provider.open(Map.of());
    assertThatThrownBy(() -> provider.navigate(context, URI.create("http://www.zhipin.com/")))
        .isInstanceOf(BrowserProviderException.class);
    assertThatThrownBy(
            () -> provider.click(context, new ProviderElementRef(UUID.randomUUID(), "foreign")))
        .isInstanceOfSatisfying(
            BrowserProviderException.class,
            exception -> assertThat(exception.reasonCode()).isEqualTo("CROSS_PROVIDER_REFERENCE"));

    provider.close(context);
    provider.close(context);
    assertThat(provider.calls().stream().filter("close"::equals)).hasSize(1);
    assertThatThrownBy(() -> provider.snapshot(context))
        .isInstanceOf(BrowserProviderException.class);
  }

  private static Stream<Arguments> providers() {
    return Stream.of(
        Arguments.of(new FakeBrowserProvider(BrowserProviderType.FAKE)),
        Arguments.of(new FakeBrowserProvider(BrowserProviderType.PLAYWRIGHT_MCP)),
        Arguments.of(new FakeBrowserProvider(BrowserProviderType.KIMI_WEBBRIDGE)));
  }
}
