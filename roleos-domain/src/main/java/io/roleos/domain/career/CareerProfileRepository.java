package io.roleos.domain.career;

import io.roleos.domain.career.profile.CareerProfile;
import java.util.Optional;

/** 职业档案持久化端口，领域不依赖 JPA。 */
public interface CareerProfileRepository {
  Optional<CareerProfile> findByUserId(UserId userId);

  CareerProfile save(CareerProfile profile);
}
