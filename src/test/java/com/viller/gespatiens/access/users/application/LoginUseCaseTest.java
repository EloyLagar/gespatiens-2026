package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria y un emisor de tokens falso
class LoginUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final AccessTokenIssuer issuer = user -> new AccessToken("token-de-" + user.getEmail(), Instant.EPOCH);
    private final LoginUseCase useCase = new LoginUseCase(repository, hasher, issuer);

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

    @Test
    void missingFieldsAreInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(null, "secreto123"));
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("ana@vientos.es", null));
    }
}
