package io.roleos.domain.career.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.roleos.domain.career.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResumeImportTest {

  @Test
  void candidateStartsWaitingAndCanOnlyBeDecidedOnce() {
    FactCandidate candidate = candidate();

    assertEquals(CandidateReviewStatus.WAITING_CONFIRMATION, candidate.status());
    candidate.decide(
        new CandidateDecision(
            candidate.id(), CandidateDecisionType.CONFIRM, null, UUID.randomUUID(), Instant.EPOCH));

    assertEquals(CandidateReviewStatus.CONFIRMED, candidate.status());
    assertThrows(
        IllegalStateException.class,
        () ->
            candidate.decide(
                new CandidateDecision(
                    candidate.id(),
                    CandidateDecisionType.REJECT,
                    null,
                    UUID.randomUUID(),
                    Instant.EPOCH)));
  }

  @Test
  void editRequiresPayloadAndOtherDecisionsRejectIt() {
    UUID candidateId = UUID.randomUUID();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CandidateDecision(
                candidateId, CandidateDecisionType.EDIT, "", UUID.randomUUID(), Instant.EPOCH));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CandidateDecision(
                candidateId,
                CandidateDecisionType.CONFIRM,
                "{}",
                UUID.randomUUID(),
                Instant.EPOCH));
  }

  @Test
  void importKeepsCandidatesSeparateFromConfirmedFacts() {
    FactCandidate candidate = candidate();
    ResumeImport resumeImport =
        new ResumeImport(
            UUID.randomUUID(),
            new UserId(UUID.randomUUID()),
            "fixture://controlled/resume",
            Instant.EPOCH,
            List.of(candidate));

    assertEquals(
        CandidateReviewStatus.WAITING_CONFIRMATION, resumeImport.candidates().getFirst().status());
  }

  private FactCandidate candidate() {
    return new FactCandidate(UUID.randomUUID(), "SKILL", "{\"displayName\":\"Java\"}", "skills:1");
  }
}
