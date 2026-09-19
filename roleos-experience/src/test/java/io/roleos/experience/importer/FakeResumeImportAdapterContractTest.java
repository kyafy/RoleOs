package io.roleos.experience.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FakeResumeImportAdapterContractTest {
  private final FakeResumeImportAdapter adapter = new FakeResumeImportAdapter();

  @Test
  void returnsOnlyCompleteFixtureCandidatesAndReportsPartialFailures() {
    ResumeImportResult result =
        adapter.extract(
            new ResumeImportRequest(
                "fixture://resume/one",
                List.of(
                    new ResumeImportFixture("EXPERIENCE", "{\"title\":\"Engineer\"}", "section:1"),
                    new ResumeImportFixture("SKILL", null, "section:2"))));

    assertEquals(1, result.candidates().size());
    assertEquals("WAITING_CONFIRMATION", result.candidates().getFirst().status().name());
    assertEquals(1, result.failures().size());
    assertTrue(result.failures().getFirst().contains("missing"));
  }
}
