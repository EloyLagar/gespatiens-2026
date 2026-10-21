package com.viller.gespatiens.access.users.infrastructure.security;

import com.viller.gespatiens.access.users.application.LoginAttempts;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Los intentos en memoria: la aplicación corre en un solo servidor y, si se reinicia, empezar de cero no es un
// riesgo (el bloqueo frena a quien prueba contraseñas, no es un registro). Tras maxAttempts fallos seguidos, el
// email queda bloqueado durante lockout
public class InMemoryLoginAttempts implements LoginAttempts {

    private final int maxAttempts;
    private final Duration lockout;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    public InMemoryLoginAttempts(int maxAttempts, Duration lockout) {
        if (maxAttempts < 5) {
            // Decisión del centro: nunca menos de 5 intentos antes de bloquear
            throw new IllegalArgumentException("El bloqueo tiene que permitir al menos 5 intentos");
        }
        this.maxAttempts = maxAttempts;
        this.lockout = lockout;
    }

    @Override
    public Optional<Instant> blockedUntil(String email, Instant now) {
        State state = states.get(email);
        if (state == null || state.blockedUntil() == null) {
            return Optional.empty();
        }
        if (!now.isBefore(state.blockedUntil())) {
            // El bloqueo ha pasado: vuelve a tener todos los intentos
            states.remove(email);
            return Optional.empty();
        }
        return Optional.of(state.blockedUntil());
    }

    @Override
    public void recordFailure(String email, Instant now) {
        states.compute(email, (key, state) -> {
            int failures = (state == null ? 0 : state.failures()) + 1;
            return new State(failures, failures >= maxAttempts ? now.plus(lockout) : null);
        });
    }

    @Override
    public void reset(String email) {
        states.remove(email);
    }

    private record State(int failures, Instant blockedUntil) {
    }
}
