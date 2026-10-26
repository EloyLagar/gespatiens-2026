package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Theme;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// El tema (claro, oscuro o el del equipo) se guarda en el perfil del empleado, como el idioma
public class ChangeOwnThemeUseCase {

    private final UserRepository userRepository;

    public ChangeOwnThemeUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(UUID currentUserId, Theme theme) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        user.changeTheme(theme);
        return userRepository.save(user);
    }
}
