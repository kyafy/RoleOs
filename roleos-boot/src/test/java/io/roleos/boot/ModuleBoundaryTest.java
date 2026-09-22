package io.roleos.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 防止 Job/Career 领域在后续迭代中反向依赖 Browser、Web、Storage 或具体运行时。 */
class ModuleBoundaryTest {

  private static final List<String> JOB_FORBIDDEN_REFERENCES =
      List.of(
          "io.roleos.browser",
          "io.roleos.storage",
          "io.roleos.web",
          "io.modelcontextprotocol",
          "com.microsoft.playwright",
          "KimiWebBridge",
          "org.springframework.web");

  private static final List<String> CAREER_FORBIDDEN_REFERENCES =
      List.of(
          "Playwright",
          "KimiWebBridge",
          "CSS Selector",
          "XPath",
          "io.roleos.browser",
          "io.roleos.storage",
          "io.roleos.web");

  @Test
  void jobModuleMustDependOnlyOnOwnedPortsAndStableDomain() throws IOException {
    assertNoForbiddenReferences(sourceRoot("roleos-job"), JOB_FORBIDDEN_REFERENCES);
  }

  @Test
  void careerDomainMustNotContainBrowserImplementationDetails() throws IOException {
    assertNoForbiddenReferences(sourceRoot("roleos-domain"), CAREER_FORBIDDEN_REFERENCES);
  }

  @Test
  void experienceModuleMustNotDependOnAdaptersOrWeb() throws IOException {
    assertNoForbiddenReferences(
        sourceRoot("roleos-experience"),
        List.of("io.roleos.storage", "io.roleos.web", "io.roleos.browser", "io.roleos.runtime"));
  }

  private static Path sourceRoot(String module) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.isDirectory(current.resolve("roleos-domain"))) {
      current = current.getParent();
    }
    if (current == null) {
      throw new IllegalStateException("Cannot locate RoleOS repository root");
    }
    return current.resolve(module).resolve("src/main/java");
  }

  private static void assertNoForbiddenReferences(Path root, List<String> forbidden)
      throws IOException {
    try (Stream<Path> sources = Files.walk(root)) {
      List<String> violations =
          sources
              .filter(path -> path.toString().endsWith(".java"))
              .flatMap(path -> violations(path, forbidden))
              .toList();

      assertThat(violations).as("module boundary violations under %s", root).isEmpty();
    }
  }

  private static Stream<String> violations(Path path, List<String> forbidden) {
    try {
      String source = Files.readString(path);
      return forbidden.stream()
          .filter(source::contains)
          .map(reference -> path + " -> " + reference);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot inspect source file: " + path, exception);
    }
  }
}
