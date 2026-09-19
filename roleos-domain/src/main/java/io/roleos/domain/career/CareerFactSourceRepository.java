package io.roleos.domain.career;

/** Persistence port for append-only career-fact provenance. */
@FunctionalInterface
public interface CareerFactSourceRepository {
  CareerFactSource append(CareerFactSource source);
}
