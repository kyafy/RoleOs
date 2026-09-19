package io.roleos.storage.career;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Storage 层内部 Spring Data Repository。 */
interface SpringDataCareerProfileRepository extends JpaRepository<CareerProfileJpaEntity, UUID> {

  Optional<CareerProfileJpaEntity> findByUserId(UUID userId);
}
