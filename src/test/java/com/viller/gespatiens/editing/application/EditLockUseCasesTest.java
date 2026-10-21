package com.viller.gespatiens.editing.application;

import com.viller.gespatiens.editing.domain.EditLock;
import com.viller.gespatiens.editing.domain.EditLockRepository;
import com.viller.gespatiens.editing.domain.EmployeeNames;
import com.viller.gespatiens.editing.domain.LockRaceException;
import com.viller.gespatiens.editing.domain.ResourceBeingEditedException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class EditLockUseCasesTest {

    private static final String SECTION = "report:1:section:PSYCHO_DIAGNOSIS";

    private final UUID marcos = UUID.randomUUID();
    private final UUID elena = UUID.randomUUID();
    private final EmployeeNames names = id -> Optional.of(id.equals(marcos) ? "Marcos" : "Elena");
    private final InMemoryLocks locks = new InMemoryLocks();
    private final MovableClock clock = new MovableClock(Instant.parse("2026-10-21T10:00:00Z"));

    private final AcquireEditLockUseCase acquire = new AcquireEditLockUseCase(locks, names, clock);
    private final ReleaseEditLockUseCase release = new ReleaseEditLockUseCase(locks);
    private final ListEditLocksUseCase list = new ListEditLocksUseCase(locks, names, clock);
    private final EditLockGuard guard = new EditLockGuard(locks, names, clock);

    // RF8: mientras alguien edita algo, nadie más puede editarlo, y se sabe quién es
    @Test
    void whileSomeoneEditsNobodyElseCan() {
        EditLockView mine = acquire.execute(SECTION, marcos);

        ResourceBeingEditedException busy = assertThrows(ResourceBeingEditedException.class,
                () -> acquire.execute(SECTION, elena));
        assertEquals("Marcos", busy.getHolderName());
        assertTrue(mine.mine());
        assertThrows(ResourceBeingEditedException.class, () -> guard.ensureNotEditedByOther(SECTION, elena));
        assertDoesNotThrow(() -> guard.ensureNotEditedByOther(SECTION, marcos));
        // Lo demás del informe sigue libre
        assertDoesNotThrow(() -> acquire.execute("report:1:section:SOCIAL_DIAGNOSIS", elena));
    }

    // El fallo de la aplicación original: si alguien salía sin guardar, el documento se quedaba bloqueado
    @Test
    void aLockThatIsNotRenewedExpiresOnItsOwn() {
        acquire.execute(SECTION, marcos);

        clock.advanceSeconds(60);
        acquire.execute(SECTION, marcos); // la pantalla lo renueva
        clock.advanceSeconds(60);
        assertThrows(ResourceBeingEditedException.class, () -> acquire.execute(SECTION, elena));

        clock.advanceSeconds(EditLock.LEASE.toSeconds());
        EditLockView taken = acquire.execute(SECTION, elena);
        assertEquals(elena, taken.holderId());
    }

    @Test
    void releasingFreesItButOnlyTheOwnerCanRelease() {
        acquire.execute(SECTION, marcos);

        release.execute(SECTION, elena);
        assertThrows(ResourceBeingEditedException.class, () -> acquire.execute(SECTION, elena));

        release.execute(SECTION, marcos);
        assertDoesNotThrow(() -> acquire.execute(SECTION, elena));
    }

    @Test
    void theListShowsWhoIsEditingWhatInsideSomething() {
        acquire.execute(SECTION, marcos);
        acquire.execute("report:1:header", elena);
        acquire.execute("report:2:header", elena);

        List<EditLockView> inReport = list.execute("report:1:", marcos);

        assertEquals(List.of("report:1:header", SECTION), inReport.stream().map(EditLockView::resource).toList());
        assertFalse(inReport.getFirst().mine());
        assertEquals("Elena", inReport.getFirst().holderName());
        clock.advanceSeconds(EditLock.LEASE.toSeconds() + 1);
        assertTrue(list.execute("report:1:", marcos).isEmpty());
    }

    // Dos peticiones a la vez: la base de datos deja pasar a una y a la otra se le dice quién ganó
    @Test
    void aSimultaneousAcquisitionNamesTheWinner() {
        locks.raceWith(EditLock.acquire(SECTION, elena, clock.instant()));

        ResourceBeingEditedException busy = assertThrows(ResourceBeingEditedException.class,
                () -> acquire.execute(SECTION, marcos));

        assertEquals("Elena", busy.getHolderName());
    }

    @Test
    void resourcesHaveAKnownShape() {
        assertThrows(IllegalArgumentException.class, () -> acquire.execute("report 1; drop", marcos));
        assertThrows(IllegalArgumentException.class, () -> list.execute(" ", marcos));
    }

    private static final class InMemoryLocks implements EditLockRepository {

        private final Map<String, EditLock> all = new HashMap<>();
        private EditLock racer;

        // La próxima vez que se guarde, otra petición se habrá adelantado con este bloqueo
        void raceWith(EditLock lock) {
            this.racer = lock;
        }

        @Override
        public Optional<EditLock> find(String resource) {
            return Optional.ofNullable(all.get(resource));
        }

        @Override
        public EditLock save(EditLock lock) {
            if (racer != null) {
                all.put(racer.getResource(), racer);
                racer = null;
                throw new LockRaceException(null);
            }
            all.put(lock.getResource(), lock);
            return lock;
        }

        @Override
        public void delete(EditLock lock) {
            all.remove(lock.getResource());
        }

        @Override
        public List<EditLock> findAll() {
            return List.copyOf(all.values());
        }
    }

    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
