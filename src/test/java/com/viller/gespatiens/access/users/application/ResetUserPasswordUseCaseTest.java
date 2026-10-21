package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class ResetUserPasswordUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final ResetUserPasswordUseCase useCase =
            new ResetUserPasswordUseCase(repository, new FakePasswordHasher());

    @Test
    void setsNewPasswordWithoutKnowingTheOldOne() {
        User luis = repository.save(User.create("Luis", "luis@vientos.es", "600333444",
                Speciality.SOCIAL_EDUCATOR, "hashed:olvidada"));

        useCase.execute(luis.getId(), "provisional1");

        assertEquals("hashed:provisional1", repository.findById(luis.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void rejectsShortPassword() {
        User luis = repository.save(User.create("Luis", "luis@vientos.es", "600333444",
                Speciality.SOCIAL_EDUCATOR, "hashed:olvidada"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(luis.getId(), "corta"));
    }
}
