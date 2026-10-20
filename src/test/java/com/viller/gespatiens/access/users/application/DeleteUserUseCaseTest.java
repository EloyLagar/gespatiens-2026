package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.AdministratorGuard;
import com.viller.gespatiens.access.users.domain.LastAdministratorException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class DeleteUserUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final DeleteUserUseCase useCase = new DeleteUserUseCase(repository, new AdministratorGuard(repository));

    @Test
    void deletesEmployee() {
        User luis = repository.save(User.create("Luis", "luis@vientos.es", "600333444",
                Speciality.SOCIAL_EDUCATOR, "hash"));

        useCase.execute(luis.getId());

        assertTrue(repository.findById(luis.getId()).isEmpty());
    }

    @Test
    void cannotDeleteTheLastAdministrator() {
        User admin = repository.save(User.create("Admin", "admin@vientos.es", "600000000",
                Speciality.ADMINISTRATOR, "hash"));

        assertThrows(LastAdministratorException.class, () -> useCase.execute(admin.getId()));
        assertTrue(repository.findById(admin.getId()).isPresent());
    }

    @Test
    void rejectsUnknownUser() {
        assertThrows(UserNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
