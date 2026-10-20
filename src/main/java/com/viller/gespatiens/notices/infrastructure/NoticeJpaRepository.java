package com.viller.gespatiens.notices.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoticeJpaRepository extends JpaRepository<NoticeJpaEntity, UUID> {
    List<NoticeJpaEntity> findAllByOrderByCreatedAtDesc();
}
