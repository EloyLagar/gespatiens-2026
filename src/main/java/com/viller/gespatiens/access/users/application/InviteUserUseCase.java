package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;

// RF7 y memoria final: la administración da de alta al empleado y a este le llega un correo
// para crear sus credenciales de acceso
public class InviteUserUseCase {

    private final UserRepository userRepository;
    private final PasswordSetupLinks passwordSetupLinks;

    public InviteUserUseCase(UserRepository userRepository, PasswordSetupLinks passwordSetupLinks) {
        this.userRepository = userRepository;
        this.passwordSetupLinks = passwordSetupLinks;
    }

    public Invitation execute(String name, String email, String phoneNumber, Speciality speciality) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyRegisteredException(phoneNumber);
        }
        User user = userRepository.save(User.invite(name, email, phoneNumber, speciality));

        // Si el correo falla, el empleado queda dado de alta igualmente y la administración puede
        // reenviarlo desde su ficha. Deshacer el alta obligaría a rellenar el formulario otra vez
        try {
            passwordSetupLinks.issueFor(user);
            return new Invitation(user, true);
        } catch (PasswordSetupDeliveryException e) {
            return new Invitation(user, false);
        }
    }

    public record Invitation(User user, boolean emailSent) {
    }
}
