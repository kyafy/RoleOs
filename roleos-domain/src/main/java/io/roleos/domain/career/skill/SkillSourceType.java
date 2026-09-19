package io.roleos.domain.career.skill;

/** Origin of a skill statement, kept distinct from its verification state. */
public enum SkillSourceType {
  SELF_DECLARED,
  CONFIRMED_EXPERIENCE,
  CONFIRMED_PROJECT,
  PROJECT_UPGRADE,
  SOURCE_CODE
}
