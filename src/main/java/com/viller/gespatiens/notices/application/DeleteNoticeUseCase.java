package com.viller.gespatiens.notices.application;

import com.viller.gespatiens.notices.domain.AuthorDirectory;
import com.viller.gespatiens.notices.domain.Notice;
import com.viller.gespatiens.notices.domain.NoticeNotFoundException;
import com.viller.gespatiens.notices.domain.NoticeNotManageableException;
import com.viller.gespatiens.notices.domain.NoticeRepository;

import java.util.UUID;

// RF5: "la administración y su propio autor tendrán la opción de eliminar el aviso"
public class DeleteNoticeUseCase {

    private final NoticeRepository noticeRepository;
    private final AuthorDirectory authorDirectory;

    public DeleteNoticeUseCase(NoticeRepository noticeRepository, AuthorDirectory authorDirectory) {
        this.noticeRepository = noticeRepository;
        this.authorDirectory = authorDirectory;
    }

    public void execute(UUID noticeId, UUID requesterId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));
        if (!notice.canBeManagedBy(requesterId, NoticeViews.isAdministrator(authorDirectory, requesterId))) {
            throw new NoticeNotManageableException();
        }
        noticeRepository.delete(notice);
    }
}
