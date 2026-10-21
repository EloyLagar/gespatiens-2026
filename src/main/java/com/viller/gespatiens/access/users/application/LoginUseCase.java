package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

// RF3: solo los empleados registrados pueden entrar. Devuelve el token y el empleado,
// para que el frontend sepa quién es y qué apartados mostrarle sin otra petición.
// Tras varios intentos fallidos seguidos con el mismo email se bloquea un rato (LoginAttempts)
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final LoginAttempts attempts;
    private final Clock clock;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                        AccessTokenIssuer accessTokenIssuer, LoginAttempts attempts, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.attempts = attempts;
        this.clock = clock;
    }

    public Session execute(String email, String password) {
        if (email == null || password == null) {
            throw new InvalidCredentialsException();
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        Instant now = clock.instant();
        // Mientras dura el bloqueo ni se comprueba la contraseña: tampoco la correcta abre
        attempts.blockedUntil(key, now).ifPresent(until -> {
            throw new LoginBlockedException(until, now);
        });
        User user = userRepository.findByEmail(email.trim())
                // El invitado que aún no ha creado su contraseña no puede entrar
                .filter(User::hasPassword)
                .filter(u -> passwordHasher.matches(password, u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            attempts.recordFailure(key, now);
            throw new InvalidCredentialsException();
        }
        attempts.reset(key);
        return new Session(user, accessTokenIssuer.issueFor(user));
    }

    public record Session(User user, AccessToken token) {
    }
}
