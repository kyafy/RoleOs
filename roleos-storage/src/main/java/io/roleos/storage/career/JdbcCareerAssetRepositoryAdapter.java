package io.roleos.storage.career;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.CareerAssetRepository;
import io.roleos.domain.career.ConfirmationStatus;
import io.roleos.domain.career.ProvenanceType;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.profile.Experience;
import io.roleos.domain.career.profile.ExperienceType;
import io.roleos.domain.career.profile.Project;
import io.roleos.domain.career.profile.ProjectNature;
import io.roleos.domain.career.skill.SelfAssessmentLevel;
import io.roleos.domain.career.skill.Skill;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 使用用户归属条件实现职业资产持久化。 */
@Repository
@SuppressWarnings("PMD.UnusedFormalParameter")
public class JdbcCareerAssetRepositoryAdapter implements CareerAssetRepository {
  private final JdbcTemplate jdbcTemplate;
  private final CareerUserJdbcSupport users;

  public JdbcCareerAssetRepositoryAdapter(JdbcTemplate jdbcTemplate, CareerUserJdbcSupport users) {
    this.jdbcTemplate = jdbcTemplate;
    this.users = users;
  }

  @Override
  public List<Experience> findExperiences(UserId userId) {
    return jdbcTemplate.query(
        "SELECT * FROM experience WHERE user_id=? ORDER BY created_at,id",
        this::experience,
        userId.value());
  }

  @Override
  public Optional<Experience> findExperience(UserId userId, UUID id) {
    return jdbcTemplate
        .query(
            "SELECT * FROM experience WHERE user_id=? AND id=?",
            this::experience,
            userId.value(),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public Experience save(Experience value) {
    users.ensure(value.userId());
    jdbcTemplate.update(
        "INSERT INTO experience(id,user_id,experience_type,title,organization,incomplete,provenance_type,confirmation_status,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO UPDATE SET experience_type=EXCLUDED.experience_type,title=EXCLUDED.title,organization=EXCLUDED.organization,incomplete=EXCLUDED.incomplete,updated_at=EXCLUDED.updated_at,version=experience.version+1 WHERE experience.user_id=EXCLUDED.user_id",
        value.id(),
        value.userId().value(),
        value.type().name(),
        value.title(),
        value.organization(),
        value.incomplete(),
        value.provenanceType().name(),
        value.confirmationStatus().name(),
        Timestamp.from(value.auditFields().createdAt()),
        Timestamp.from(value.auditFields().updatedAt()),
        value.auditFields().version());
    return findExperience(value.userId(), value.id()).orElseThrow();
  }

  @Override
  public boolean deleteExperience(UserId userId, UUID id) {
    return jdbcTemplate.update(
            "DELETE FROM experience WHERE user_id=? AND id=?", userId.value(), id)
        == 1;
  }

  @Override
  public List<Project> findProjects(UserId userId) {
    return jdbcTemplate.query(
        "SELECT * FROM career_project WHERE user_id=? ORDER BY created_at,id",
        this::project,
        userId.value());
  }

  @Override
  public Optional<Project> findProject(UserId userId, UUID id) {
    return jdbcTemplate
        .query(
            "SELECT * FROM career_project WHERE user_id=? AND id=?",
            this::project,
            userId.value(),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public Project save(Project value) {
    users.ensure(value.userId());
    jdbcTemplate.update(
        "INSERT INTO career_project(id,user_id,name,project_nature,historical_source,provenance_type,confirmation_status,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name,project_nature=EXCLUDED.project_nature,historical_source=EXCLUDED.historical_source,updated_at=EXCLUDED.updated_at,version=career_project.version+1 WHERE career_project.user_id=EXCLUDED.user_id",
        value.id(),
        value.userId().value(),
        value.name(),
        value.nature().name(),
        value.historicalSource(),
        value.provenanceType().name(),
        value.confirmationStatus().name(),
        Timestamp.from(value.auditFields().createdAt()),
        Timestamp.from(value.auditFields().updatedAt()),
        value.auditFields().version());
    return findProject(value.userId(), value.id()).orElseThrow();
  }

  @Override
  public boolean deleteProject(UserId userId, UUID id) {
    return jdbcTemplate.update(
            "DELETE FROM career_project WHERE user_id=? AND id=?", userId.value(), id)
        == 1;
  }

  @Override
  public List<Skill> findSkills(UserId userId) {
    return jdbcTemplate.query(
        "SELECT * FROM skill WHERE user_id=? ORDER BY normalized_name,id",
        this::skill,
        userId.value());
  }

  @Override
  public Optional<Skill> findSkill(UserId userId, UUID id) {
    return jdbcTemplate
        .query("SELECT * FROM skill WHERE user_id=? AND id=?", this::skill, userId.value(), id)
        .stream()
        .findFirst();
  }

  @Override
  public Skill save(Skill value) {
    users.ensure(value.userId());
    jdbcTemplate.update(
        "INSERT INTO skill(id,user_id,display_name,normalized_name,self_assessment_level,verified_level,provenance_type,confirmation_status,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO UPDATE SET display_name=EXCLUDED.display_name,normalized_name=EXCLUDED.normalized_name,self_assessment_level=EXCLUDED.self_assessment_level,updated_at=EXCLUDED.updated_at,version=skill.version+1 WHERE skill.user_id=EXCLUDED.user_id",
        value.id(),
        value.userId().value(),
        value.displayName(),
        value.normalizedName(),
        value.selfAssessmentLevel().name(),
        value.verifiedLevel() == null ? null : value.verifiedLevel().name(),
        value.provenanceType().name(),
        value.confirmationStatus().name(),
        Timestamp.from(value.auditFields().createdAt()),
        Timestamp.from(value.auditFields().updatedAt()),
        value.auditFields().version());
    return findSkill(value.userId(), value.id()).orElseThrow();
  }

  @Override
  public boolean deleteSkill(UserId userId, UUID id) {
    return jdbcTemplate.update("DELETE FROM skill WHERE user_id=? AND id=?", userId.value(), id)
        == 1;
  }

  private Experience experience(ResultSet row, int number) throws SQLException {
    return new Experience(
        row.getObject("id", UUID.class),
        new UserId(row.getObject("user_id", UUID.class)),
        ExperienceType.valueOf(row.getString("experience_type")),
        row.getString("title"),
        row.getString("organization"),
        row.getBoolean("incomplete"),
        ProvenanceType.valueOf(row.getString("provenance_type")),
        ConfirmationStatus.valueOf(row.getString("confirmation_status")),
        audit(row));
  }

  private Project project(ResultSet row, int number) throws SQLException {
    return new Project(
        row.getObject("id", UUID.class), new UserId(row.getObject("user_id", UUID.class)),
        row.getString("name"), ProjectNature.valueOf(row.getString("project_nature")),
        row.getString("historical_source"),
            ProvenanceType.valueOf(row.getString("provenance_type")),
        ConfirmationStatus.valueOf(row.getString("confirmation_status")), audit(row));
  }

  private Skill skill(ResultSet row, int number) throws SQLException {
    String verified = row.getString("verified_level");
    return new Skill(
        row.getObject("id", UUID.class),
        new UserId(row.getObject("user_id", UUID.class)),
        row.getString("display_name"),
        row.getString("normalized_name"),
        SelfAssessmentLevel.valueOf(row.getString("self_assessment_level")),
        verified == null ? null : SelfAssessmentLevel.valueOf(verified),
        ProvenanceType.valueOf(row.getString("provenance_type")),
        ConfirmationStatus.valueOf(row.getString("confirmation_status")),
        audit(row));
  }

  private AuditFields audit(ResultSet row) throws SQLException {
    return new AuditFields(
        row.getTimestamp("created_at").toInstant(),
        row.getTimestamp("updated_at").toInstant(),
        row.getLong("version"));
  }
}
