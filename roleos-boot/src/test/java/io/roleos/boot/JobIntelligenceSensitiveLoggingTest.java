package io.roleos.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 静态门禁：Job/Browser 日志语句不得引用正文、凭据或 Provider 私有句柄。 */
class JobIntelligenceSensitiveLoggingTest {

  private static final Pattern LOG_STATEMENT =
      Pattern.compile("LOGGER\\.(?:trace|debug|info|warn|error)\\s*\\(.*?\\);", Pattern.DOTALL);
  private static final List<String> FORBIDDEN =
      List.of("rawjd", "rawpayload", "cookie", "token", "handle", "accessibilitytext");

  @Test
  void jobAndBrowserLogsExcludeSensitivePayloads() throws IOException {
    Path root = repositoryRoot();
    try (Stream<Path> files =
        Stream.concat(
            Files.walk(root.resolve("roleos-job/src/main/java")),
            Files.walk(root.resolve("roleos-browser/src/main/java")))) {
      List<String> violations =
          files
              .filter(path -> path.toString().endsWith(".java"))
              .flatMap(JobIntelligenceSensitiveLoggingTest::violations)
              .toList();
      assertThat(violations).isEmpty();
    }
  }

  @Test
  void productionLoggingUsesJsonAsyncRollingPolicy() throws IOException {
    String config =
        Files.readString(
            repositoryRoot().resolve("roleos-boot/src/main/resources/logback-spring.xml"));
    assertThat(config)
        .contains("LogstashEncoder")
        .contains("AsyncAppender")
        .contains("SizeAndTimeBasedRollingPolicy")
        .contains("maxHistory")
        .contains("totalSizeCap");
  }

  private static Stream<String> violations(Path path) {
    try {
      String source = Files.readString(path);
      var matcher = LOG_STATEMENT.matcher(source);
      Stream.Builder<String> result = Stream.builder();
      while (matcher.find()) {
        String statement = matcher.group().toLowerCase(Locale.ROOT);
        FORBIDDEN.stream()
            .filter(statement::contains)
            .forEach(word -> result.add(path + " -> " + word));
      }
      return result.build();
    } catch (IOException exception) {
      throw new IllegalStateException("无法检查日志源码: " + path, exception);
    }
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.isDirectory(current.resolve("roleos-job"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("无法定位 RoleOS 仓库根目录");
    return current;
  }
}
