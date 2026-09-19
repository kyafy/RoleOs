package io.roleos.domain.career;

import io.roleos.domain.career.profile.Experience;
import io.roleos.domain.career.profile.Project;
import io.roleos.domain.career.skill.Skill;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 经历、项目和技能的持久化端口；所有读取和删除均强制携带用户归属。 */
public interface CareerAssetRepository {
  List<Experience> findExperiences(UserId userId);

  Optional<Experience> findExperience(UserId userId, UUID id);

  Experience save(Experience experience);

  boolean deleteExperience(UserId userId, UUID id);

  List<Project> findProjects(UserId userId);

  Optional<Project> findProject(UserId userId, UUID id);

  Project save(Project project);

  boolean deleteProject(UserId userId, UUID id);

  List<Skill> findSkills(UserId userId);

  Optional<Skill> findSkill(UserId userId, UUID id);

  Skill save(Skill skill);

  boolean deleteSkill(UserId userId, UUID id);
}
