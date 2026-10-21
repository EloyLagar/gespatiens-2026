package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.Signature;
import com.viller.gespatiens.access.users.domain.SignatureNotFoundException;
import com.viller.gespatiens.access.users.domain.SignatureRepository;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.domain.UserRepository;

import java.time.Clock;
import java.util.UUID;

// Subir, ver y quitar la firma escaneada de un empleado. Cada uno gestiona la suya desde su perfil y la
// administración puede hacerlo por cualquiera (quién puede qué lo comprueba SecurityConfig)
public class SignatureUseCase {

    private final UserRepository userRepository;
    private final SignatureRepository signatureRepository;
    private final Clock clock;

    public SignatureUseCase(UserRepository userRepository, SignatureRepository signatureRepository, Clock clock) {
        this.userRepository = userRepository;
        this.signatureRepository = signatureRepository;
        this.clock = clock;
    }

    public Signature upload(UUID userId, byte[] png) {
        ensureExists(userId);
        return signatureRepository.save(Signature.upload(userId, png, clock.instant()));
    }

    public Signature get(UUID userId) {
        ensureExists(userId);
        return signatureRepository.findByUser(userId).orElseThrow(() -> new SignatureNotFoundException(userId));
    }

    public void remove(UUID userId) {
        ensureExists(userId);
        signatureRepository.deleteByUser(userId);
    }

    private void ensureExists(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
    }
}
