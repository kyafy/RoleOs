package io.roleos.storage.career;

import io.roleos.domain.career.UserId;
import io.roleos.domain.career.skill.Capability;
import io.roleos.domain.career.skill.CapabilityLinks;
import io.roleos.domain.career.skill.SkillProvenanceRepository;
import io.roleos.domain.career.skill.SkillSource;
import io.roleos.domain.career.skill.SkillSourceType;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** User-scoped JDBC persistence for skill provenance and explicit capability links. */
@Repository
public class JdbcSkillProvenanceRepositoryAdapter implements SkillProvenanceRepository {
  private final JdbcTemplate jdbc;
  private final CareerUserJdbcSupport users;

  public JdbcSkillProvenanceRepositoryAdapter(JdbcTemplate jdbc, CareerUserJdbcSupport users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  @Override
  public List<SkillSource> findSources(UserId userId, UUID skillId) {
    return jdbc.query(
        "SELECT ss.* FROM skill_sources ss JOIN skill s ON s.id=ss.skill_id WHERE s.user_id=? AND ss.skill_id=? ORDER BY recorded_at,id",
        (r, n) ->
            new SkillSource(
                r.getObject("id", UUID.class),
                skillId,
                SkillSourceType.valueOf(r.getString("source_type")),
                r.getString("supporting_asset_reference"),
                r.getTimestamp("recorded_at").toInstant()),
        userId.value(),
        skillId);
  }

  @Override
  public SkillSource saveSource(UserId userId, SkillSource source) {
    users.ensure(userId);
    jdbc.update(
        "INSERT INTO skill_sources(id,skill_id,source_type,supporting_asset_reference,recorded_at) SELECT ?,?,?,?,? WHERE EXISTS (SELECT 1 FROM skill WHERE id=? AND user_id=?)",
        source.id(),
        source.skillId(),
        source.type().name(),
        source.supportingAssetReference(),
        Timestamp.from(source.recordedAt()),
        source.skillId(),
        userId.value());
    return source;
  }

  @Override
  public List<Capability> findCapabilities(UserId userId) {
    return jdbc.query(
        "SELECT id,user_id,name FROM capabilities WHERE user_id=? ORDER BY normalized_name,id",
        (r, n) ->
            new Capability(
                r.getObject("id", UUID.class),
                new UserId(r.getObject("user_id", UUID.class)),
                r.getString("name")),
        userId.value());
  }

  @Override
  public Optional<Capability> findCapability(UserId userId, UUID id) {
    return findCapabilities(userId).stream().filter(c -> c.id().equals(id)).findFirst();
  }

  @Override
  public Capability saveCapability(Capability value) {
    users.ensure(value.userId());
    jdbc.update(
        "INSERT INTO capabilities(id,user_id,name,normalized_name) VALUES (?,?,?,?) ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name,normalized_name=EXCLUDED.normalized_name WHERE capabilities.user_id=EXCLUDED.user_id",
        value.id(),
        value.userId().value(),
        value.name(),
        value.name().trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT));
    return value;
  }

  @Override
  public CapabilityLinks links(UserId userId, UUID id) {
    requireCapability(userId, id);
    return new CapabilityLinks(
        id,
        ids("capability_skills", "skill_id", id),
        ids("capability_experiences", "experience_id", id),
        ids("capability_projects", "project_id", id));
  }

  @Override
  @Transactional
  public CapabilityLinks replaceLinks(UserId userId, CapabilityLinks links) {
    requireCapability(userId, links.capabilityId());
    replace("capability_skills", "skill_id", links.capabilityId(), links.skillIds());
    replace("capability_experiences", "experience_id", links.capabilityId(), links.experienceIds());
    replace("capability_projects", "project_id", links.capabilityId(), links.projectIds());
    return links;
  }

  private void requireCapability(UserId userId, UUID id) {
    if (findCapability(userId, id).isEmpty()) throw new IllegalArgumentException("能力不存在或不属于当前用户");
  }

  private List<UUID> ids(String table, String column, UUID id) {
    return jdbc.query(
        "SELECT " + column + " FROM " + table + " WHERE capability_id=?",
        (r, n) -> r.getObject(1, UUID.class),
        id);
  }

  private void replace(String table, String column, UUID capabilityId, List<UUID> ids) {
    jdbc.update("DELETE FROM " + table + " WHERE capability_id=?", capabilityId);
    for (UUID id : ids)
      jdbc.update(
          "INSERT INTO " + table + "(capability_id," + column + ") VALUES (?,?)", capabilityId, id);
  }
}
