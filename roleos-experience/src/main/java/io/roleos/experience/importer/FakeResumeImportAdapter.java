package io.roleos.experience.importer;

import io.roleos.domain.career.importing.FactCandidate;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Deterministic V1 adapter. It accepts only supplied structured fixtures and never infers or fills
 * a missing career field.
 */
@Component
public final class FakeResumeImportAdapter implements ResumeImportPort {
  @Override
  public ResumeImportResult extract(ResumeImportRequest request) {
    List<FactCandidate> candidates = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    for (int index = 0; index < request.fixtures().size(); index++) {
      ResumeImportFixture fixture = request.fixtures().get(index);
      if (!isComplete(fixture)) {
        failures.add("fixture " + index + " is missing candidateType, payload, or sourceLocation");
        continue;
      }
      candidates.add(
          new FactCandidate(
              UUID.nameUUIDFromBytes(
                  (request.controlledSourceReference() + ":" + index)
                      .getBytes(StandardCharsets.UTF_8)),
              fixture.candidateType(),
              fixture.payload(),
              fixture.sourceLocation()));
    }
    return new ResumeImportResult(candidates, failures);
  }

  private boolean isComplete(ResumeImportFixture fixture) {
    return fixture != null
        && hasText(fixture.candidateType())
        && hasText(fixture.payload())
        && hasText(fixture.sourceLocation());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
