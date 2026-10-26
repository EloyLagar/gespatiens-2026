package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.Theme;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class ChangeOwnThemeUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final ChangeOwnThemeUseCase useCase = new ChangeOwnThemeUseCase(repository);

    @Test
    void newEmployeesFollowTheirDevice() {
        assertEquals(Theme.SYSTEM,
                User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash").getTheme());
    }

    @Test
    void savesTheChosenTheme() {
        User ana = repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash"));

        useCase.execute(ana.getId(), Theme.DARK);

        assertEquals(Theme.DARK, repository.findById(ana.getId()).orElseThrow().getTheme());
    }

    @Test
    void aThemeIsRequired() {
        User ana = repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(ana.getId(), null));
    }
}
