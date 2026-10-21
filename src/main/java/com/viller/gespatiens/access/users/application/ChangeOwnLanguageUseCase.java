package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Language;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// RNF9: el idioma de la aplicación se guarda en el perfil del empleado, así lo encuentra igual en cualquier equipo
public class ChangeOwnLanguageUseCase {

    private final UserRepository userRepository;

    public ChangeOwnLanguageUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(UUID currentUserId, Language language) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        user.changeLanguage(language);
        return userRepository.save(user);
    }
}
