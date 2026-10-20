package com.viller.gespatiens.notices.application;

import com.viller.gespatiens.notices.domain.AuthorDirectory;
import com.viller.gespatiens.notices.domain.Notice;
import com.viller.gespatiens.notices.domain.NoticeNotFoundException;
import com.viller.gespatiens.notices.domain.NoticeNotManageableException;
import com.viller.gespatiens.notices.domain.NoticeRepository;

import java.time.Clock;
import java.util.UUID;

// La memoria dice que los avisos "podrán ser modificados" sin decir por quién: lo mismo que el borrado,
// su autor y la administración. Que cualquiera reescribiera un aviso ajeno cambiaría lo que firmó su autor
public class EditNoticeUseCase {

    private final NoticeRepository noticeRepository;
    private final AuthorDirectory authorDirectory;
    private final Clock clock;

    public EditNoticeUseCase(NoticeRepository noticeRepository, AuthorDirectory authorDirectory, Clock clock) {
        this.noticeRepository = noticeRepository;
        this.authorDirectory = authorDirectory;
        this.clock = clock;
    }

    public NoticeView execute(UUID noticeId, UUID editorId, String text) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));
        boolean editorIsAdmin = NoticeViews.isAdministrator(authorDirectory, editorId);
        if (!notice.canBeManagedBy(editorId, editorIsAdmin)) {
            throw new NoticeNotManageableException();
        }
        notice.edit(text, clock.instant());
        Notice saved = noticeRepository.save(notice);
        return NoticeViews.of(saved, saved.getAuthorId().flatMap(authorDirectory::find), editorId, editorIsAdmin);
    }
}
