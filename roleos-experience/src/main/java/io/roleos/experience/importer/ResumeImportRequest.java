package io.roleos.experience.importer;

import java.util.List;
import java.util.Objects;

/** Controlled fixture input for the V1 fake importer. Never place raw resume content in logs. */
public record ResumeImportRequest(
    String controlledSourceReference, List<ResumeImportFixture> fixtures) {
  public ResumeImportRequest {
    if (controlledSourceReference == null || controlledSourceReference.isBlank()) {
      throw new IllegalArgumentException("controlledSourceReference must not be blank");
    }
    fixtures = List.copyOf(Objects.requireNonNull(fixtures, "fixtures"));
  }
}
