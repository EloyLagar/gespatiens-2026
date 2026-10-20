package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class UpdateOwnProfileUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final UpdateOwnProfileUseCase useCase = new UpdateOwnProfileUseCase(repository);

    @Test
    void updatesNameAndPhoneButKeepsEmailAndSpeciality() {
        repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash"));

        User updated = useCase.execute("ana@vientos.es", "Ana María", "600999888");

        assertEquals("Ana María", updated.getName());
        assertEquals("600999888", updated.getPhoneNumber());
        assertEquals("ana@vientos.es", updated.getEmail());
        assertEquals(Speciality.PSYCHOLOGIST, updated.getSpeciality());
    }

    @Test
    void keepingOwnPhoneIsNotADuplicate() {
        repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash"));

        assertDoesNotThrow(() -> useCase.execute("ana@vientos.es", "Ana María", "600111222"));
    }

    @Test
    void rejectsPhoneOfAnotherEmployee() {
        repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash"));
        repository.save(User.create("Luis", "luis@vientos.es", "600333444", Speciality.SOCIAL_EDUCATOR, "hash"));

        assertThrows(PhoneNumberAlreadyRegisteredException.class, () ->
                useCase.execute("ana@vientos.es", "Ana", "600333444"));
    }

    @Test
    void rejectsBlankName() {
        repository.save(User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute("ana@vientos.es", " ", "600111222"));
    }
}
