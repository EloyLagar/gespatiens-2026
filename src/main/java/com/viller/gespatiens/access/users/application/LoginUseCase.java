package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.application.LoginEvents.LoginAttempted;
import com.viller.gespatiens.access.users.application.LoginEvents.Outcome;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// RF3: solo los empleados registrados pueden entrar. Devuelve el token y el empleado,
// para que el frontend sepa quién es y qué apartados mostrarle sin otra petición.
// Tras varios intentos fallidos seguidos con el mismo email se bloquea un rato (LoginAttempts).
// Cada intento, bueno o malo, se avisa a LoginEvents para el registro de accesos
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final LoginAttempts attempts;
    private final LoginEvents events;
    private final Clock clock;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                        AccessTokenIssuer accessTokenIssuer, LoginAttempts attempts, LoginEvents events,
                        Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.attempts = attempts;
        this.events = events;
        this.clock = clock;
    }

    public Session execute(String email, String password, LoginOrigin origin) {
        if (email == null || password == null) {
            throw new InvalidCredentialsException();
        }
        LoginOrigin from = origin == null ? LoginOrigin.UNKNOWN : origin;
        String key = email.trim().toLowerCase(Locale.ROOT);
        Instant now = clock.instant();
        Optional<User> account = userRepository.findByEmail(email.trim());
        UUID accountId = account.map(User::getId).orElse(null);
        // Mientras dura el bloqueo ni se comprueba la contraseña: tampoco la correcta abre
        Optional<Instant> blockedUntil = attempts.blockedUntil(key, now);
        if (blockedUntil.isPresent()) {
            events.attempted(new LoginAttempted(now, key, accountId, Outcome.BLOCKED, from));
            throw new LoginBlockedException(blockedUntil.get(), now);
        }
        User user = account
                // El invitado que aún no ha creado su contraseña no puede entrar
                .filter(User::hasPassword)
                .filter(u -> passwordHasher.matches(password, u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            attempts.recordFailure(key, now);
            Outcome outcome = attempts.blockedUntil(key, now).isPresent() ? Outcome.LOCKED : Outcome.FAILURE;
            events.attempted(new LoginAttempted(now, key, accountId, outcome, from));
            throw new InvalidCredentialsException();
        }
        attempts.reset(key);
        Session session = new Session(user, accessTokenIssuer.issueFor(user));
        events.attempted(new LoginAttempted(now, key, user.getId(), Outcome.SUCCESS, from));
        return session;
    }

    public record Session(User user, AccessToken token) {
    }
}
