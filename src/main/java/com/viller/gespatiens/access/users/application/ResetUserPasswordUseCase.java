package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// La administración pone una contraseña nueva a un empleado que ha olvidado la suya.
// Como el hash no se puede deshacer, es la única forma de recuperar el acceso
public class ResetUserPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ResetUserPasswordUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public void execute(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        PasswordPolicy.check(newPassword);
        user.changePasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
    }
}
