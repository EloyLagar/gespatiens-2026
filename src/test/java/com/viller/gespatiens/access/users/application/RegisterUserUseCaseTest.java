package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class RegisterUserUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final RegisterUserUseCase useCase = new RegisterUserUseCase(repository, new FakePasswordHasher());

    @Test
    void registersUserWithHashedPassword() {
        User user = useCase.execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "secreto123");

        assertEquals("hashed:secreto123", user.getPasswordHash());
        assertNotEquals("secreto123", user.getPasswordHash());
        assertEquals(user, repository.findByEmail("ana@vientos.es").orElseThrow());
    }

    @Test
    void rejectsDuplicateEmail() {
        useCase.execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "secreto123");

        assertThrows(EmailAlreadyRegisteredException.class, () ->
                useCase.execute("Otra", "ana@vientos.es", "600999999", Speciality.SOCIAL_WORKER, "secreto123"));
    }

    @Test
    void rejectsDuplicatePhoneNumber() {
        useCase.execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "secreto123");

        assertThrows(PhoneNumberAlreadyRegisteredException.class, () ->
                useCase.execute("Otra", "otra@vientos.es", "600111222", Speciality.SOCIAL_WORKER, "secreto123"));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "corta"));
    }
}
