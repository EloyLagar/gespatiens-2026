package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.Test;

import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChange;
import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChanged;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class ResetUserPasswordUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final UUID MARTA = UUID.randomUUID();
    private static final LoginOrigin ORIGIN = new LoginOrigin("10.0.0.2", "Edge");
    private final RecordingAccessEvents events = new RecordingAccessEvents();
    private final ResetUserPasswordUseCase useCase = new ResetUserPasswordUseCase(repository,
            new FakePasswordHasher(), events, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void setsNewPasswordWithoutKnowingTheOldOne() {
        User luis = repository.save(User.create("Luis", "luis@vientos.es", "600333444",
                Speciality.SOCIAL_EDUCATOR, "hashed:olvidada"));

        useCase.execute(MARTA, luis.getId(), "provisional1", ORIGIN);

        assertEquals("hashed:provisional1", repository.findById(luis.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void rejectsShortPassword() {
        User luis = repository.save(User.create("Luis", "luis@vientos.es", "600333444",
                Speciality.SOCIAL_EDUCATOR, "hashed:olvidada"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(MARTA, luis.getId(), "corta", ORIGIN));
    }

    // Queda anotado quién de la administración la puso
    @Test
    void theResetIsReportedWithTheAdministratorWhoDidIt() {
        User luis = repository.save(User.create("Luis", "luis@vientos.es", "600333444",
                Speciality.SOCIAL_EDUCATOR, "hashed:olvidada"));

        useCase.execute(MARTA, luis.getId(), "provisional1", ORIGIN);

        assertEquals(List.of(new PasswordChanged(NOW, luis.getId(), "luis@vientos.es", PasswordChange.RESET_BY_ADMIN,
                MARTA, ORIGIN)), events.passwords);
    }
}
