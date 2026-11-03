package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.application.LoginEvents.LoginAttempted;
import com.viller.gespatiens.access.users.application.LoginEvents.Outcome;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viller.gespatiens.access.users.infrastructure.security.InMemoryLoginAttempts;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria y un emisor de tokens falso
class LoginUseCaseTest {

    private static final LoginOrigin ORIGIN = new LoginOrigin("192.168.1.37", "Firefox");

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final AccessTokenIssuer issuer = user -> new AccessToken("token-de-" + user.getEmail(), Instant.EPOCH);
    private final List<LoginAttempted> events = new ArrayList<>();
    private Instant now = Instant.parse("2026-10-21T10:00:00Z");
    private final Clock clock = new Clock() {
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
    };
    private final LoginUseCase useCase = new LoginUseCase(repository, hasher, issuer,
            new InMemoryLoginAttempts(5, Duration.ofMinutes(15)), events::add, clock);
    private User ana;

    @BeforeEach
    void registerAna() {
        ana = User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, hasher.hash("secreto123"));
        repository.save(ana);
    }

    @Test
    void validCredentialsReturnTokenAndUser() {
        LoginUseCase.Session session = useCase.execute("ana@vientos.es", "secreto123", ORIGIN);

        assertEquals("token-de-ana@vientos.es", session.token().value());
        assertEquals("Ana", session.user().getName());
    }

    @Test
    void ignoresSurroundingSpacesInEmail() {
        assertEquals("Ana", useCase.execute("  ana@vientos.es ", "secreto123", ORIGIN).user().getName());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() {
        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("ana@vientos.es", "otra-cosa", ORIGIN));
        InvalidCredentialsException unknownEmail = assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("nadie@vientos.es", "secreto123", ORIGIN));

        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void invitedEmployeeCannotSignInBeforeCreatingThePassword() {
        repository.save(User.invite("Luis", "luis@vientos.es", "600333444", Speciality.SOCIAL_EDUCATOR));

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("luis@vientos.es", "cualquiera", ORIGIN));
    }

    // Decisión del centro: al menos 5 intentos. Al sexto, ni la contraseña buena entra hasta que pase el bloqueo
    @Test
    void fiveFailuresInARowBlockTheEmailForAWhile() {
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal", ORIGIN));
        }
        LoginBlockedException blocked = assertThrows(LoginBlockedException.class,
                () -> useCase.execute("ANA@vientos.es ", "secreto123", ORIGIN));
        assertEquals(Instant.parse("2026-10-21T10:15:00Z"), blocked.getUntil());

        now = now.plus(Duration.ofMinutes(15));
        assertEquals("Ana", useCase.execute("ana@vientos.es", "secreto123", ORIGIN).user().getName());
    }

    @Test
    void aSuccessfulLoginStartsTheCountAgain() {
        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal", ORIGIN));
        }
        useCase.execute("ana@vientos.es", "secreto123", ORIGIN);
        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal", ORIGIN));
        }
        assertEquals("Ana", useCase.execute("ana@vientos.es", "secreto123", ORIGIN).user().getName());
    }

    // El bloqueo no delata qué emails existen: un email inventado se bloquea igual
    @Test
    void unknownEmailsAreBlockedTheSameWay() {
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("nadie@vientos.es", "x", ORIGIN));
        }
        assertThrows(LoginBlockedException.class, () -> useCase.execute("nadie@vientos.es", "x", ORIGIN));
    }

    @Test
    void neverFewerThanFiveAttempts() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryLoginAttempts(3, Duration.ofMinutes(15)));
    }

    @Test
    void missingFieldsAreInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(null, "secreto123", ORIGIN));
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", null, ORIGIN));
    }

    // Registro de accesos: cada intento se avisa, con el email normalizado, el empleado y desde dónde
    @Test
    void aSuccessfulLoginIsReportedWithTheEmployeeAndTheOrigin() {
        useCase.execute(" ana@vientos.es", "secreto123", ORIGIN);

        assertEquals(List.of(new LoginAttempted(now, "ana@vientos.es", ana.getId(), Outcome.SUCCESS, ORIGIN)), events);
    }

    // Con la contraseña mal se sabe de quién era la cuenta: así se ve que alguien la ha probado
    @Test
    void aWrongPasswordIsReportedAsAFailureOfThatEmployee() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal", ORIGIN));

        assertEquals(List.of(new LoginAttempted(now, "ana@vientos.es", ana.getId(), Outcome.FAILURE, ORIGIN)), events);
    }

    @Test
    void anUnknownEmailIsReportedWithoutEmployee() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("nadie@vientos.es", "x", ORIGIN));

        assertEquals(1, events.size());
        assertEquals("nadie@vientos.es", events.get(0).email());
        assertNull(events.get(0).userId());
        assertEquals(Outcome.FAILURE, events.get(0).outcome());
    }

    // El quinto fallo es el que bloquea; lo que se intente después, con el email bloqueado, se anota aparte
    @Test
    void theFailureThatLocksAndTheAttemptsWhileLockedAreReportedApart() {
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal", ORIGIN));
        }
        assertThrows(LoginBlockedException.class, () -> useCase.execute("ana@vientos.es", "secreto123", ORIGIN));

        assertEquals(List.of(Outcome.FAILURE, Outcome.FAILURE, Outcome.FAILURE, Outcome.FAILURE, Outcome.LOCKED,
                Outcome.BLOCKED), events.stream().map(LoginAttempted::outcome).toList());
        assertTrue(events.stream().allMatch(e -> ana.getId().equals(e.userId())));
    }

    // Una petición sin cuerpo no llega a ser un intento con email: no hay nada que anotar
    @Test
    void missingFieldsAreNotReported() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(null, "secreto123", ORIGIN));

        assertTrue(events.isEmpty());
    }

    @Test
    void aMissingOriginIsReportedAsUnknown() {
        useCase.execute("ana@vientos.es", "secreto123", null);

        assertEquals(LoginOrigin.UNKNOWN, events.get(0).origin());
    }
}
