package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// El empleado ha acabado o se ha saltado el tutorial de bienvenida: ya no le sale al entrar, en ningún equipo
public class MarkOwnTutorialSeenUseCase {

    private final UserRepository userRepository;

    public MarkOwnTutorialSeenUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        user.markTutorialSeen();
        return userRepository.save(user);
    }
}
