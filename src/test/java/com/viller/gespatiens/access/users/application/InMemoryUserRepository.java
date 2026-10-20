package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Repositorio en memoria compartido por los tests de los casos de uso de usuarios
class InMemoryUserRepository implements UserRepository {

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
    public List<User> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(User::getName))
                .toList();
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

    @Override
    public boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id) {
        return users.values().stream()
                .anyMatch(u -> !u.getId().equals(id) && u.getPhoneNumber().equals(phoneNumber));
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return users.values().stream()
                .anyMatch(u -> !u.getId().equals(id) && u.getEmail().equals(email));
    }

    @Override
    public long countBySpeciality(Speciality speciality) {
        return users.values().stream().filter(u -> u.getSpeciality() == speciality).count();
    }

    @Override
    public void delete(User user) {
        users.remove(user.getId());
    }
}
