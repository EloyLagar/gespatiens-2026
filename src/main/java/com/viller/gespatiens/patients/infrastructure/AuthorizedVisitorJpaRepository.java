package com.viller.gespatiens.patients.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthorizedVisitorJpaRepository extends JpaRepository<AuthorizedVisitorJpaEntity, UUID> {

    List<AuthorizedVisitorJpaEntity> findByPatientIdOrderByNameAsc(UUID patientId);
}
