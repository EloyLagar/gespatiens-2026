package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);
    List<UserJpaEntity> findAllByOrderByName();
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);
    boolean existsByEmailAndIdNot(String email, UUID id);
    long countBySpeciality(Speciality speciality);
}
