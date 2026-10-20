package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.AdministratorGuard;
import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// RF7: la administración edita el perfil de cualquier empleado, incluidos email y especialidad
public class EditUserUseCase {

    private final UserRepository userRepository;
    private final AdministratorGuard administratorGuard;

    public EditUserUseCase(UserRepository userRepository, AdministratorGuard administratorGuard) {
        this.userRepository = userRepository;
        this.administratorGuard = administratorGuard;
    }

    public User execute(UUID userId, String name, String email, String phoneNumber, Speciality speciality) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        if (phoneNumber != null && userRepository.existsByPhoneNumberAndIdNot(phoneNumber, userId)) {
            throw new PhoneNumberAlreadyRegisteredException(phoneNumber);
        }
        if (speciality != Speciality.ADMINISTRATOR) {
            administratorGuard.ensureNotLastAdministrator(user);
        }
        user.changeName(name);
        user.changeEmail(email);
        user.changePhoneNumber(phoneNumber);
        user.changeSpeciality(speciality);
        return userRepository.save(user);
    }
}
