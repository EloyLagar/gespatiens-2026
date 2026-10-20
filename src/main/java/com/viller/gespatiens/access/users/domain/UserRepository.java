package com.viller.gespatiens.access.users.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    // Ordenados por nombre
    List<User> findAll();
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    // Al editar un perfil: ¿lo tiene otro empleado distinto del que se edita?
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);
}
