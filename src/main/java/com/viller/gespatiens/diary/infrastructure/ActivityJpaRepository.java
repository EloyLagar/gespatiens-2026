package com.viller.gespatiens.diary.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ActivityJpaRepository extends JpaRepository<ActivityJpaEntity, UUID> {
    List<ActivityJpaEntity> findByDateOrderByStartTime(LocalDate date);
}
