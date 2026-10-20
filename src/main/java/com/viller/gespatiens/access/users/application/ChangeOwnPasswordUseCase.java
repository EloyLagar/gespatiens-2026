package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.InvalidCurrentPasswordException;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// RF4: el empleado cambia su contraseña. Se pide la actual aunque ya esté autenticado,
// para que nadie pueda cambiarla desde una sesión que se ha dejado abierta
public class ChangeOwnPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ChangeOwnPasswordUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public void execute(UUID currentUserId, String currentPassword, String newPassword) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        if (currentPassword == null || !passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        PasswordPolicy.check(newPassword);
        user.changePasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
    }
}
