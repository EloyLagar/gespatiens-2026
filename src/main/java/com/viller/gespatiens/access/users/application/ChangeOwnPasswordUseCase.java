package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChange;
import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChanged;
import com.viller.gespatiens.access.users.domain.InvalidCurrentPasswordException;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.time.Clock;
import java.util.UUID;

// RF4: el empleado cambia su contraseña. Se pide la actual aunque ya esté autenticado,
// para que nadie pueda cambiarla desde una sesión que se ha dejado abierta. El cambio, y el intento con la
// contraseña actual equivocada, se avisan al registro de accesos
public class ChangeOwnPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessEvents events;
    private final Clock clock;

    public ChangeOwnPasswordUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                                    AccessEvents events, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.events = events;
        this.clock = clock;
    }

    public void execute(UUID currentUserId, String currentPassword, String newPassword, LoginOrigin origin) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        if (currentPassword == null || !passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            notify(user, PasswordChange.CHANGE_REJECTED, origin);
            throw new InvalidCurrentPasswordException();
        }
        PasswordPolicy.check(newPassword);
        user.changePasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
        notify(user, PasswordChange.CHANGED, origin);
    }

    private void notify(User user, PasswordChange kind, LoginOrigin origin) {
        events.passwordChanged(new PasswordChanged(clock.instant(), user.getId(), user.getEmail(), kind, null,
                origin == null ? LoginOrigin.UNKNOWN : origin));
    }
}
