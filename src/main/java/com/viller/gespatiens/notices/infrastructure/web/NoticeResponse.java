package com.viller.gespatiens.notices.infrastructure.web;

import com.viller.gespatiens.notices.application.NoticeView;
import com.viller.gespatiens.notices.domain.Author;
import com.viller.gespatiens.notices.domain.Notice;

import java.time.Instant;
import java.util.UUID;

// DTO de salida. author es null si el empleado ya no está en la plantilla
public record NoticeResponse(
        UUID id,
        String text,
        Instant createdAt,
        Instant editedAt,
        AuthorResponse author,
        // Si es del empleado que pide la lista: la memoria pide diferenciar los propios de los ajenos
        boolean mine,
        // Si puede modificarlo y eliminarlo (su autor o la administración)
        boolean manageable) {

    public record AuthorResponse(UUID id, String name, String speciality, boolean administrator) {
        static AuthorResponse from(Author author) {
            return new AuthorResponse(author.id(), author.name(), author.speciality(), author.administrator());
        }
    }

    public static NoticeResponse from(NoticeView view) {
        Notice notice = view.notice();
        return new NoticeResponse(
                notice.getId(),
                notice.getText(),
                notice.getCreatedAt(),
                notice.getEditedAt().orElse(null),
                view.author().map(AuthorResponse::from).orElse(null),
                view.mine(),
                view.manageable());
    }
}
