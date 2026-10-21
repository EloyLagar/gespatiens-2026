package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;

// RF3: solo los empleados registrados pueden entrar. Devuelve el token y el empleado,
// para que el frontend sepa quién es y qué apartados mostrarle sin otra petición
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                        AccessTokenIssuer accessTokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public Session execute(String email, String password) {
        if (email == null || password == null) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(email.trim())
                // El invitado que aún no ha creado su contraseña no puede entrar
                .filter(User::hasPassword)
                .filter(u -> passwordHasher.matches(password, u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new Session(user, accessTokenIssuer.issueFor(user));
    }

    public record Session(User user, AccessToken token) {
    }
}
