package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.InvalidCurrentPasswordException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChange;
import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChanged;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class ChangeOwnPasswordUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final LoginOrigin ORIGIN = new LoginOrigin("192.168.1.37", "Firefox");
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final RecordingAccessEvents events = new RecordingAccessEvents();
    private final ChangeOwnPasswordUseCase useCase = new ChangeOwnPasswordUseCase(repository, hasher, events,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private User ana;

    @BeforeEach
    void registerAna() {
        ana = repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST,
                hasher.hash("antigua123")));
    }

    private String storedHash() {
        return repository.findByEmail("ana@vientos.es").orElseThrow().getPasswordHash();
    }

    @Test
    void changesPasswordWhenCurrentOneIsCorrect() {
        useCase.execute(ana.getId(), "antigua123", "nueva12345", ORIGIN);

        assertEquals("hashed:nueva12345", storedHash());
    }

    @Test
    void rejectsWrongCurrentPassword() {
        assertThrows(InvalidCurrentPasswordException.class, () ->
                useCase.execute(ana.getId(), "otra-cosa", "nueva12345", ORIGIN));
        assertEquals("hashed:antigua123", storedHash());
    }

    @Test
    void rejectsShortNewPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(ana.getId(), "antigua123", "corta", ORIGIN));
        assertEquals("hashed:antigua123", storedHash());
    }

    // Registro de accesos: el cambio queda anotado con desde dónde se hizo
    @Test
    void theChangeIsReportedToTheAccessLog() {
        useCase.execute(ana.getId(), "antigua123", "nueva12345", ORIGIN);

        assertEquals(List.of(new PasswordChanged(NOW, ana.getId(), "ana@vientos.es", PasswordChange.CHANGED, null,
                ORIGIN)), events.passwords);
    }

    // Quien prueba desde una sesión ajena sin saber la contraseña actual también deja rastro
    @Test
    void aWrongCurrentPasswordIsReportedAsRejected() {
        assertThrows(InvalidCurrentPasswordException.class, () ->
                useCase.execute(ana.getId(), "otra-cosa", "nueva12345", ORIGIN));

        assertEquals(PasswordChange.CHANGE_REJECTED, events.passwords.get(0).kind());
    }

    // Una contraseña nueva demasiado corta no cambia nada: no hay nada que anotar
    @Test
    void aShortNewPasswordIsNotReported() {
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(ana.getId(), "antigua123", "corta", ORIGIN));

        assertTrue(events.passwords.isEmpty());
    }
}
