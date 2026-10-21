package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.util.UUID;

// La administración vuelve a enviar el correo: al invitado que no lo recibió o lo dejó caducar, o al
// empleado que ha olvidado su contraseña, que así la cambia sin que nadie más la conozca
public class SendPasswordSetupLinkUseCase {

    private final UserRepository userRepository;
    private final PasswordSetupLinks passwordSetupLinks;

    public SendPasswordSetupLinkUseCase(UserRepository userRepository, PasswordSetupLinks passwordSetupLinks) {
        this.userRepository = userRepository;
        this.passwordSetupLinks = passwordSetupLinks;
    }

    public void execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        passwordSetupLinks.issueFor(user);
    }
}
