package com.viller.gespatiens.notices.application;

import com.viller.gespatiens.notices.domain.Author;
import com.viller.gespatiens.notices.domain.AuthorDirectory;
import com.viller.gespatiens.notices.domain.Notice;
import com.viller.gespatiens.notices.domain.NoticeNotFoundException;
import com.viller.gespatiens.notices.domain.NoticeNotManageableException;
import com.viller.gespatiens.notices.domain.NoticeRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con repositorio y directorio de autores en memoria
class NoticeUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-10-20T09:30:00Z");

    private final Author admin = new Author(UUID.randomUUID(), "Administrador", "ADMINISTRATOR", true);
    private final Author pablo = new Author(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR", false);
    private final Author laura = new Author(UUID.randomUUID(), "Laura", "MEDICAL_TEAM", false);

    private final InMemoryNoticeRepository repository = new InMemoryNoticeRepository();
    private final AuthorDirectory directory = new FixedAuthorDirectory(admin, pablo, laura);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final PublishNoticeUseCase publish = new PublishNoticeUseCase(repository, directory, clock);
    private final ListNoticesUseCase list = new ListNoticesUseCase(repository, directory);
    private final EditNoticeUseCase edit = new EditNoticeUseCase(repository, directory, clock);
    private final DeleteNoticeUseCase delete = new DeleteNoticeUseCase(repository, directory);

    @Test
    void anyEmployeePublishesAndTheNoticeIsTheirs() {
        NoticeView view = publish.execute(pablo.id(), "Salida terapéutica a las 17:00");

        assertEquals("Pablo", view.author().orElseThrow().name());
        assertEquals(NOW, view.notice().getCreatedAt());
        assertTrue(view.mine());
        assertTrue(view.manageable());
    }

    @Test
    void listShowsNewestFirstAndWhichOnesAreMine() {
        repository.save(Notice.reconstitute(UUID.randomUUID(), pablo.id(), "Primero", NOW.minusSeconds(60), null));
        repository.save(Notice.reconstitute(UUID.randomUUID(), admin.id(), "Segundo", NOW, null));

        List<NoticeView> seenByPablo = list.execute(pablo.id());

        assertEquals("Segundo", seenByPablo.get(0).notice().getText());
        assertTrue(seenByPablo.get(0).author().orElseThrow().administrator());
        assertFalse(seenByPablo.get(0).mine());
        assertFalse(seenByPablo.get(0).manageable());
        assertTrue(seenByPablo.get(1).mine());
    }

    @Test
    void administrationCanManageEveryNotice() {
        repository.save(Notice.reconstitute(UUID.randomUUID(), pablo.id(), "De Pablo", NOW, null));

        assertTrue(list.execute(admin.id()).get(0).manageable());
    }

    @Test
    void noticeOfADeletedEmployeeIsListedWithoutAuthor() {
        repository.save(Notice.reconstitute(UUID.randomUUID(), null, "De alguien que ya no está", NOW, null));

        NoticeView view = list.execute(pablo.id()).get(0);
        assertTrue(view.author().isEmpty());
        assertFalse(view.mine());
    }

    @Test
    void authorEditsTheirNotice() {
        UUID id = publish.execute(pablo.id(), "Reunión a las 10").notice().getId();

        NoticeView edited = edit.execute(id, pablo.id(), "Reunión a las 11");

        assertEquals("Reunión a las 11", edited.notice().getText());
        assertEquals(NOW, edited.notice().getEditedAt().orElseThrow());
    }

    @Test
    void colleagueCannotEditOrDeleteSomeoneElsesNotice() {
        UUID id = publish.execute(pablo.id(), "Reunión a las 10").notice().getId();

        assertThrows(NoticeNotManageableException.class, () -> edit.execute(id, laura.id(), "Cambiado"));
        assertThrows(NoticeNotManageableException.class, () -> delete.execute(id, laura.id()));
        assertEquals("Reunión a las 10", repository.findById(id).orElseThrow().getText());
    }

    @Test
    void authorAndAdministrationCanDelete() {
        UUID mine = publish.execute(pablo.id(), "Mío").notice().getId();
        UUID other = publish.execute(laura.id(), "De Laura").notice().getId();

        delete.execute(mine, pablo.id());
        delete.execute(other, admin.id());

        assertTrue(repository.findAllNewestFirst().isEmpty());
    }

    @Test
    void unknownNoticeIsNotFound() {
        assertThrows(NoticeNotFoundException.class, () -> delete.execute(UUID.randomUUID(), admin.id()));
    }

    private static class InMemoryNoticeRepository implements NoticeRepository {
        private final Map<UUID, Notice> notices = new HashMap<>();

        @Override
        public Notice save(Notice notice) {
            notices.put(notice.getId(), notice);
            return notice;
        }

        @Override
        public Optional<Notice> findById(UUID id) {
            return Optional.ofNullable(notices.get(id));
        }

        @Override
        public List<Notice> findAllNewestFirst() {
            return notices.values().stream()
                    .sorted(Comparator.comparing(Notice::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public void delete(Notice notice) {
            notices.remove(notice.getId());
        }
    }

    private static class FixedAuthorDirectory implements AuthorDirectory {
        private final Map<UUID, Author> authors = new HashMap<>();

        FixedAuthorDirectory(Author... all) {
            for (Author a : all) authors.put(a.id(), a);
        }

        @Override
        public Optional<Author> find(UUID employeeId) {
            return Optional.ofNullable(authors.get(employeeId));
        }

        @Override
        public Map<UUID, Author> findAll(Collection<UUID> employeeIds) {
            Map<UUID, Author> found = new HashMap<>();
            employeeIds.forEach(id -> find(id).ifPresent(a -> found.put(id, a)));
            return found;
        }
    }
}
