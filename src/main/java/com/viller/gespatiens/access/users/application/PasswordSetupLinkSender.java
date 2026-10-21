package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.User;

import java.time.Instant;

// Puerto: hace llegar al empleado el enlace para crear su contraseña. La aplicación no sabe si es
// un correo por SMTP o una línea en el log (MailPasswordSetupLinkSender, LoggingPasswordSetupLinkSender)
public interface PasswordSetupLinkSender {
    // Lanza PasswordSetupDeliveryException si no se ha podido enviar
    void send(User user, String token, Instant expiresAt);
}
