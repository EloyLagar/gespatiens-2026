package com.viller.gespatiens.notices.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoticeRepository {
    Notice save(Notice notice);
    Optional<Notice> findById(UUID id);
    // Del más reciente al más antiguo
    List<Notice> findAllNewestFirst();
    void delete(Notice notice);
}
