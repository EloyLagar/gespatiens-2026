package com.viller.gespatiens.notices.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoticeTest {

    private static final Instant MORNING = Instant.parse("2026-10-20T08:00:00Z");
    private static final Instant NOON = Instant.parse("2026-10-20T12:00:00Z");
    private final UUID author = UUID.randomUUID();

    @Test
    void publishedNoticeHasAuthorDateAndTrimmedText() {
        Notice notice = Notice.publish(author, "  Revisión de medicación a las 11  ", MORNING);

        assertEquals(author, notice.getAuthorId().orElseThrow());
        assertEquals("Revisión de medicación a las 11", notice.getText());
        assertEquals(MORNING, notice.getCreatedAt());
        assertTrue(notice.getEditedAt().isEmpty());
    }

    @Test
    void rejectsEmptyOrTooLongText() {
        assertThrows(IllegalArgumentException.class, () -> Notice.publish(author, "   ", MORNING));
        assertThrows(IllegalArgumentException.class, () -> Notice.publish(author, null, MORNING));
        assertThrows(IllegalArgumentException.class, () -> Notice.publish(author, "a".repeat(2001), MORNING));
    }

    @Test
    void editingChangesTextAndMarksItAsEdited() {
        Notice notice = Notice.publish(author, "Reunión a las 10", MORNING);

        notice.edit("Reunión a las 11", NOON);

        assertEquals("Reunión a las 11", notice.getText());
        assertEquals(NOON, notice.getEditedAt().orElseThrow());
    }

    @Test
    void savingTheSameTextDoesNotMarkItAsEdited() {
        Notice notice = Notice.publish(author, "Reunión a las 10", MORNING);

        notice.edit(" Reunión a las 10 ", NOON);

        assertTrue(notice.getEditedAt().isEmpty());
    }

    @Test
    void onlyItsAuthorOrAdministrationCanManageIt() {
        Notice notice = Notice.publish(author, "Reunión a las 10", MORNING);
        UUID colleague = UUID.randomUUID();

        assertTrue(notice.canBeManagedBy(author, false));
        assertTrue(notice.canBeManagedBy(colleague, true));
        assertFalse(notice.canBeManagedBy(colleague, false));
    }

    @Test
    void noticeOfADeletedEmployeeIsKeptAndOnlyAdministrationManagesIt() {
        Notice orphan = Notice.reconstitute(UUID.randomUUID(), null, "Aviso antiguo", MORNING, null);

        assertTrue(orphan.getAuthorId().isEmpty());
        assertFalse(orphan.canBeManagedBy(author, false));
        assertTrue(orphan.canBeManagedBy(author, true));
    }
}
