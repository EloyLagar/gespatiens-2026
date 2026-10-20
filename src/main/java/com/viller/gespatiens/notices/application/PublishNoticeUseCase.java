package com.viller.gespatiens.notices.application;

import com.viller.gespatiens.notices.domain.AuthorDirectory;
import com.viller.gespatiens.notices.domain.Notice;
import com.viller.gespatiens.notices.domain.NoticeRepository;

import java.time.Clock;
import java.util.UUID;

// RF5: cualquier empleado puede publicar avisos para el resto del personal
public class PublishNoticeUseCase {

    private final NoticeRepository noticeRepository;
    private final AuthorDirectory authorDirectory;
    private final Clock clock;

    public PublishNoticeUseCase(NoticeRepository noticeRepository, AuthorDirectory authorDirectory, Clock clock) {
        this.noticeRepository = noticeRepository;
        this.authorDirectory = authorDirectory;
        this.clock = clock;
    }

    public NoticeView execute(UUID authorId, String text) {
        Notice notice = noticeRepository.save(Notice.publish(authorId, text, clock.instant()));
        return NoticeViews.of(notice, authorDirectory.find(authorId), authorId,
                NoticeViews.isAdministrator(authorDirectory, authorId));
    }
}
