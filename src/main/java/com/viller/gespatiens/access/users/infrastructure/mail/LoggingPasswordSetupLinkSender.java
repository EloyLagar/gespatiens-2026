package com.viller.gespatiens.access.users.infrastructure.mail;

import com.viller.gespatiens.access.users.application.PasswordSetupLinkSender;
import com.viller.gespatiens.access.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

// Sin servidor de correo configurado (MAIL_HOST vacío), el enlace se escribe en el log. Sirve para
// desarrollar y probar sin SMTP: se copia el enlace de la consola de Spring Boot
public class LoggingPasswordSetupLinkSender implements PasswordSetupLinkSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordSetupLinkSender.class);

    private final PasswordSetupLinkFactory links;

    public LoggingPasswordSetupLinkSender(PasswordSetupLinkFactory links) {
        this.links = links;
    }

    @Override
    public void send(User user, String token, Instant expiresAt) {
        log.warn("MAIL_HOST no definido: no se envía el correo. Enlace para que {} cree su contraseña "
                + "(caduca el {}): {}", user.getEmail(), links.format(expiresAt), links.linkFor(token));
    }
}
