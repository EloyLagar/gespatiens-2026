package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.InvalidCurrentPasswordException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class ChangeOwnPasswordUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final ChangeOwnPasswordUseCase useCase = new ChangeOwnPasswordUseCase(repository, hasher);

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
        useCase.execute(ana.getId(), "antigua123", "nueva12345");

        assertEquals("hashed:nueva12345", storedHash());
    }

    @Test
    void rejectsWrongCurrentPassword() {
        assertThrows(InvalidCurrentPasswordException.class, () ->
                useCase.execute(ana.getId(), "otra-cosa", "nueva12345"));
        assertEquals("hashed:antigua123", storedHash());
    }

    @Test
    void rejectsShortNewPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(ana.getId(), "antigua123", "corta"));
        assertEquals("hashed:antigua123", storedHash());
    }
}
