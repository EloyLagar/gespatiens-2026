package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

// RF4: el empleado cambia sus propios datos. Ni el email ni la especialidad:
// el email es su identificador de acceso y la especialidad decide sus permisos
public class UpdateOwnProfileUseCase {

    private final UserRepository userRepository;

    public UpdateOwnProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(String currentEmail, String name, String phoneNumber) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new UserNotFoundException(currentEmail));
        if (phoneNumber != null && userRepository.existsByPhoneNumberAndIdNot(phoneNumber, user.getId())) {
            throw new PhoneNumberAlreadyRegisteredException(phoneNumber);
        }
        user.changeName(name);
        user.changePhoneNumber(phoneNumber);
        return userRepository.save(user);
    }
}
