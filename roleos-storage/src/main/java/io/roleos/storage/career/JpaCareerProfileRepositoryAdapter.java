package io.roleos.storage.career;

import io.roleos.domain.career.AuditFields;
import io.roleos.domain.career.CareerProfileRepository;
import io.roleos.domain.career.UserId;
import io.roleos.domain.career.profile.CareerProfile;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 将 CareerProfile Repository Port 适配到 JPA；领域模型不依赖持久化实现。 */
@Repository
public class JpaCareerProfileRepositoryAdapter implements CareerProfileRepository {

  private final SpringDataCareerProfileRepository repository;
  private final CareerUserJdbcSupport users;

  public JpaCareerProfileRepositoryAdapter(
      SpringDataCareerProfileRepository repository, CareerUserJdbcSupport users) {
    this.repository = repository;
    this.users = users;
  }

  @Override
  public Optional<CareerProfile> findByUserId(UserId userId) {
    return repository.findByUserId(userId.value()).map(this::toDomain);
  }

  @Override
  public CareerProfile save(CareerProfile profile) {
    users.ensure(profile.userId());
    return toDomain(repository.save(toEntity(profile)));
  }

  private CareerProfileJpaEntity toEntity(CareerProfile profile) {
    AuditFields audit = profile.auditFields();
    return new CareerProfileJpaEntity(
        profile.id(),
        profile.userId().value(),
        profile.displayName(),
        profile.careerDirection(),
        profile.targetRolePreference(),
        profile.provenanceType(),
        profile.confirmationStatus(),
        audit.createdAt(),
        audit.updatedAt(),
        audit.version());
  }

  private CareerProfile toDomain(CareerProfileJpaEntity entity) {
    return new CareerProfile(
        entity.id(),
        new UserId(entity.userId()),
        entity.displayName(),
        entity.careerDirection(),
        entity.targetRolePreference(),
        entity.provenanceType(),
        entity.confirmationStatus(),
        new AuditFields(entity.createdAt(), entity.updatedAt(), entity.version()));
  }
}
