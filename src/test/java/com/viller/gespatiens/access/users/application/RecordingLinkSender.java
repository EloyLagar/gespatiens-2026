package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Guarda los "correos" enviados para que el test pueda abrir el enlace. Con failing = true simula que el SMTP falla
class RecordingLinkSender implements PasswordSetupLinkSender {

    record Sent(User user, String token, Instant expiresAt) {
    }

    final List<Sent> sent = new ArrayList<>();
    boolean failing;

    @Override
    public void send(User user, String token, Instant expiresAt) {
        if (failing) {
            throw new PasswordSetupDeliveryException(user.getEmail(), new RuntimeException("SMTP caído"));
        }
        sent.add(new Sent(user, token, expiresAt));
    }

    String lastToken() {
        return sent.getLast().token();
    }
}
