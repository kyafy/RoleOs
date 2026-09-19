package io.roleos.domain.career.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillProvenanceTest {
  @Test
  void selfDeclaredSourceIsNotVerificationAndLevelRemainsSeparate() {
    Skill skill =
        new Skill(
            UUID.randomUUID(),
            UserId.random(),
            "  LangGraph ",
            "langgraph",
            SelfAssessmentLevel.USED,
            null,
            ProvenanceType.USER_INPUT,
            ConfirmationStatus.CONFIRMED,
            new AuditFields(Instant.EPOCH, Instant.EPOCH, 0));
    SkillSource source =
        new SkillSource(
            UUID.randomUUID(),
            skill.id(),
            SkillSourceType.SELF_DECLARED,
            "skill:" + skill.id(),
            Instant.EPOCH);

    assertEquals(SkillSourceType.SELF_DECLARED, source.type());
    assertEquals(SelfAssessmentLevel.USED, skill.selfAssessmentLevel());
    assertNull(skill.verifiedLevel());
  }
}
