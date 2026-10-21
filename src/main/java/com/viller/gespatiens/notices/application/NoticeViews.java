package com.viller.gespatiens.notices.application;

import com.viller.gespatiens.notices.domain.Author;
import com.viller.gespatiens.notices.domain.AuthorDirectory;
import com.viller.gespatiens.notices.domain.Notice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Arma las NoticeView de un empleado. Lo comparten el listado y los casos de uso que devuelven un aviso
final class NoticeViews {

    private NoticeViews() {
    }

    static boolean isAdministrator(AuthorDirectory directory, UUID employeeId) {
        return directory.find(employeeId).map(Author::administrator).orElse(false);
    }

    static NoticeView of(Notice notice, Optional<Author> author, UUID viewerId, boolean viewerIsAdmin) {
        boolean mine = notice.getAuthorId().map(viewerId::equals).orElse(false);
        return new NoticeView(notice, author, mine, notice.canBeManagedBy(viewerId, viewerIsAdmin));
    }

    static List<NoticeView> of(List<Notice> notices, AuthorDirectory directory, UUID viewerId) {
        Map<UUID, Author> authors = directory.findAll(
                notices.stream().flatMap(n -> n.getAuthorId().stream()).distinct().toList());
        boolean viewerIsAdmin = isAdministrator(directory, viewerId);
        return notices.stream()
                .map(n -> of(n, n.getAuthorId().map(authors::get), viewerId, viewerIsAdmin))
                .toList();
    }
}
