package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class RegisterUserUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final RegisterUserUseCase useCase = new RegisterUserUseCase(repository, raw -> "hashed:" + raw);

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

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();

        @Override
        public User save(User user) {
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public boolean existsByPhoneNumber(String phoneNumber) {
            return users.values().stream().anyMatch(u -> u.getPhoneNumber().equals(phoneNumber));
        }
    }
}
