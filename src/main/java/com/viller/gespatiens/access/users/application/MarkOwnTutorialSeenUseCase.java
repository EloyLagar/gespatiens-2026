package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// El empleado ha acabado o se ha saltado el tutorial de bienvenida: ya no le sale al entrar, en ningún equipo.
// Con everyLogin (la demo publicada, de cuentas compartidas) no se guarda: le vuelve a salir a cada visitante
public class MarkOwnTutorialSeenUseCase {

    private final UserRepository userRepository;
    private final boolean everyLogin;

    public MarkOwnTutorialSeenUseCase(UserRepository userRepository, boolean everyLogin) {
        this.userRepository = userRepository;
        this.everyLogin = everyLogin;
    }

    public User execute(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        if (everyLogin) {
            return user;
        }
        user.markTutorialSeen();
        return userRepository.save(user);
    }
}
