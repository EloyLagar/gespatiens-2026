package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChange;
import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChanged;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.time.Clock;
import java.util.UUID;

// La administración pone una contraseña nueva a un empleado que ha olvidado la suya.
// Como el hash no se puede deshacer, es la única forma de recuperar el acceso. Queda en el registro de accesos
// quién la puso
public class ResetUserPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessEvents events;
    private final Clock clock;

    public ResetUserPasswordUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                                    AccessEvents events, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.events = events;
        this.clock = clock;
    }

    public void execute(UUID actorId, UUID userId, String newPassword, LoginOrigin origin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        PasswordPolicy.check(newPassword);
        user.changePasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
        events.passwordChanged(new PasswordChanged(clock.instant(), user.getId(), user.getEmail(),
                PasswordChange.RESET_BY_ADMIN, actorId, origin == null ? LoginOrigin.UNKNOWN : origin));
    }
}
