package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PasswordSetupToken;
import com.viller.gespatiens.access.users.domain.PasswordSetupTokenRepository;
import com.viller.gespatiens.access.users.domain.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

// Genera el enlace para crear la contraseña y lo envía. Lo comparten el alta del empleado y el
// reenvío desde su ficha
public class PasswordSetupLinks {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final PasswordSetupTokenRepository tokenRepository;
    private final PasswordSetupLinkSender sender;
    private final Clock clock;
    private final Duration validity;

    public PasswordSetupLinks(PasswordSetupTokenRepository tokenRepository, PasswordSetupLinkSender sender,
                              Clock clock, Duration validity) {
        this.tokenRepository = tokenRepository;
        this.sender = sender;
        this.clock = clock;
        this.validity = validity;
    }

    // El enlace nuevo anula los anteriores: solo vale el del último correo
    public void issueFor(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = clock.instant().plus(validity);

        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.save(PasswordSetupToken.issue(hash(token), user.getId(), expiresAt));
        sender.send(user, token, expiresAt);
    }

    // SHA-256 basta: el token ya es aleatorio y largo, no hace falta un hash lento como el de las contraseñas
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
