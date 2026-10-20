package com.viller.gespatiens.notices.infrastructure;

import com.viller.gespatiens.notices.domain.Notice;
import com.viller.gespatiens.notices.domain.NoticeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NoticeRepositoryJpaAdapter implements NoticeRepository {

    private final NoticeJpaRepository noticeJpaRepository;

    public NoticeRepositoryJpaAdapter(NoticeJpaRepository noticeJpaRepository) {
        this.noticeJpaRepository = noticeJpaRepository;
    }

    @Override
    public Notice save(Notice notice) {
        return toDomain(noticeJpaRepository.save(toEntity(notice)));
    }

    @Override
    public Optional<Notice> findById(UUID id) {
        return noticeJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notice> findAllNewestFirst() {
        return noticeJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Notice notice) {
        noticeJpaRepository.deleteById(notice.getId());
    }

    private NoticeJpaEntity toEntity(Notice notice) {
        NoticeJpaEntity entity = new NoticeJpaEntity();
        entity.setId(notice.getId());
        entity.setAuthorId(notice.getAuthorId().orElse(null));
        entity.setText(notice.getText());
        entity.setCreatedAt(notice.getCreatedAt());
        entity.setEditedAt(notice.getEditedAt().orElse(null));
        return entity;
    }

    private Notice toDomain(NoticeJpaEntity entity) {
        return Notice.reconstitute(
                entity.getId(),
                entity.getAuthorId(),
                entity.getText(),
                entity.getCreatedAt(),
                entity.getEditedAt());
    }
}
