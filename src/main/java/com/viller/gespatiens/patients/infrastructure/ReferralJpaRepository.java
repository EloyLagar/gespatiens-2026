package com.viller.gespatiens.patients.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReferralJpaRepository extends JpaRepository<ReferralJpaEntity, UUID> {
}
