package com.viller.gespatiens.notices.application;

import com.viller.gespatiens.notices.domain.AuthorDirectory;
import com.viller.gespatiens.notices.domain.NoticeRepository;

import java.util.List;
import java.util.UUID;

// La pantalla de inicio: todos los avisos, del más reciente al más antiguo, vistos por un empleado
public class ListNoticesUseCase {

    private final NoticeRepository noticeRepository;
    private final AuthorDirectory authorDirectory;

    public ListNoticesUseCase(NoticeRepository noticeRepository, AuthorDirectory authorDirectory) {
        this.noticeRepository = noticeRepository;
        this.authorDirectory = authorDirectory;
    }

    public List<NoticeView> execute(UUID viewerId) {
        return NoticeViews.of(noticeRepository.findAllNewestFirst(), authorDirectory, viewerId);
    }
}
