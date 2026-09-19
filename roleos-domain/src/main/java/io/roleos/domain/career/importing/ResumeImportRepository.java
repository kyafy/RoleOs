package io.roleos.domain.career.importing;

import io.roleos.domain.career.UserId;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for controlled import batches and their user-reviewed candidates. */
public interface ResumeImportRepository {
  ResumeImport save(ResumeImport resumeImport);

  Optional<ResumeImport> findImport(UserId userId, UUID importId);

  Optional<FactCandidate> findCandidate(UserId userId, UUID candidateId);

  CandidateDecision saveDecision(UserId userId, CandidateDecision decision);
}
