package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class MarkOwnTutorialSeenUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final MarkOwnTutorialSeenUseCase useCase = new MarkOwnTutorialSeenUseCase(repository);

    @Test
    void newEmployeesHaveNotSeenTheTutorial() {
        assertFalse(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash").hasSeenTutorial());
        assertFalse(User.invite("Luis", "luis@vientos.es", "600333444", Speciality.SOCIAL_EDUCATOR).hasSeenTutorial());
    }

    @Test
    void marksTheTutorialAsSeen() {
        User ana = repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash"));

        useCase.execute(ana.getId());

        assertTrue(repository.findById(ana.getId()).orElseThrow().hasSeenTutorial());
    }

    @Test
    void unknownEmployeeFails() {
        assertThrows(UserNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
