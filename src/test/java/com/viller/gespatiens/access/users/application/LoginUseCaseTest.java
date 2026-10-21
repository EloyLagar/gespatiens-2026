package com.viller.gespatiens.access.users.application;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria y un emisor de tokens falso
class LoginUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final AccessTokenIssuer issuer = user -> new AccessToken("token-de-" + user.getEmail(), Instant.EPOCH);
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
            new InMemoryLoginAttempts(5, Duration.ofMinutes(15)), clock);

    @BeforeEach
    void registerAna() {
        repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST,
                hasher.hash("secreto123")));
    }

    @Test
    void validCredentialsReturnTokenAndUser() {
        LoginUseCase.Session session = useCase.execute("ana@vientos.es", "secreto123");

        assertEquals("token-de-ana@vientos.es", session.token().value());
        assertEquals("Ana", session.user().getName());
    }

    @Test
    void ignoresSurroundingSpacesInEmail() {
        assertEquals("Ana", useCase.execute("  ana@vientos.es ", "secreto123").user().getName());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() {
        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("ana@vientos.es", "otra-cosa"));
        InvalidCredentialsException unknownEmail = assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("nadie@vientos.es", "secreto123"));

        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void invitedEmployeeCannotSignInBeforeCreatingThePassword() {
        repository.save(User.invite("Luis", "luis@vientos.es", "600333444", Speciality.SOCIAL_EDUCATOR));

        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("luis@vientos.es", "cualquiera"));
    }

    // Decisión del centro: al menos 5 intentos. Al sexto, ni la contraseña buena entra hasta que pase el bloqueo
    @Test
    void fiveFailuresInARowBlockTheEmailForAWhile() {
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal"));
        }
        LoginBlockedException blocked = assertThrows(LoginBlockedException.class,
                () -> useCase.execute("ANA@vientos.es ", "secreto123"));
        assertEquals(Instant.parse("2026-10-21T10:15:00Z"), blocked.getUntil());

        now = now.plus(Duration.ofMinutes(15));
        assertEquals("Ana", useCase.execute("ana@vientos.es", "secreto123").user().getName());
    }

    @Test
    void aSuccessfulLoginStartsTheCountAgain() {
        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal"));
        }
        useCase.execute("ana@vientos.es", "secreto123");
        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", "mal"));
        }
        assertEquals("Ana", useCase.execute("ana@vientos.es", "secreto123").user().getName());
    }

    // El bloqueo no delata qué emails existen: un email inventado se bloquea igual
    @Test
    void unknownEmailsAreBlockedTheSameWay() {
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class, () -> useCase.execute("nadie@vientos.es", "x"));
        }
        assertThrows(LoginBlockedException.class, () -> useCase.execute("nadie@vientos.es", "x"));
    }

    @Test
    void neverFewerThanFiveAttempts() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryLoginAttempts(3, Duration.ofMinutes(15)));
    }

    @Test
    void missingFieldsAreInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(null, "secreto123"));
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", null));
    }
}
