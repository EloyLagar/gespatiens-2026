package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.AdministratorGuard;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// RF7: la administración elimina el perfil de un empleado
public class DeleteUserUseCase {

    private final UserRepository userRepository;
    private final AdministratorGuard administratorGuard;

    public DeleteUserUseCase(UserRepository userRepository, AdministratorGuard administratorGuard) {
        this.userRepository = userRepository;
        this.administratorGuard = administratorGuard;
    }

    public void execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        administratorGuard.ensureNotLastAdministrator(user);
        userRepository.delete(user);
    }
}
