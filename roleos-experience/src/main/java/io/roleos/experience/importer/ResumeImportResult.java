package io.roleos.experience.importer;

import io.roleos.domain.career.importing.FactCandidate;
import java.util.List;
import java.util.Objects;

/** Partial failures describe invalid fixture fields without manufacturing replacement values. */
public record ResumeImportResult(List<FactCandidate> candidates, List<String> failures) {
  public ResumeImportResult {
    candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
    failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
  }
}
