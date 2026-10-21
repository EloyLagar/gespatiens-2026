package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.InvalidPasswordSetupTokenException;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.PasswordSetupToken;
import com.viller.gespatiens.access.users.domain.PasswordSetupTokenRepository;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.time.Clock;

// El empleado abre el enlace del correo y crea su contraseña. Es lo único, junto al login, que se
// usa sin haber entrado: el token del enlace hace de credencial
public class PasswordSetupUseCase {

    private final PasswordSetupTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public PasswordSetupUseCase(PasswordSetupTokenRepository tokenRepository, UserRepository userRepository,
                                PasswordHasher passwordHasher, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    // Antes de enseñar el formulario: de quién es el enlace, para saludarle por su nombre
    public User check(String token) {
        return userOf(validToken(token));
    }

    public User complete(String token, String newPassword) {
        PasswordSetupToken setupToken = validToken(token);
        PasswordPolicy.check(newPassword);
        User user = userOf(setupToken);
        user.changePasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
        // El enlace es de un solo uso
        tokenRepository.deleteByUserId(user.getId());
        return user;
    }

    private PasswordSetupToken validToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidPasswordSetupTokenException();
        }
        return tokenRepository.findByTokenHash(PasswordSetupLinks.hash(token))
                .filter(t -> !t.isExpired(clock.instant()))
                .orElseThrow(InvalidPasswordSetupTokenException::new);
    }

    // Sin empleado no hay token (ON DELETE CASCADE), pero por si se borra entre las dos lecturas
    private User userOf(PasswordSetupToken token) {
        return userRepository.findById(token.getUserId())
                .orElseThrow(InvalidPasswordSetupTokenException::new);
    }
}
