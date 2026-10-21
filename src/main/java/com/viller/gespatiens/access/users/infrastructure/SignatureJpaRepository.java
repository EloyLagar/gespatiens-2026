package com.viller.gespatiens.access.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SignatureJpaRepository extends JpaRepository<SignatureJpaEntity, UUID> {
}
