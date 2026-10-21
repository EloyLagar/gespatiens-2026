package com.viller.gespatiens.access.users.infrastructure.mail;

import com.viller.gespatiens.access.users.application.PasswordSetupDeliveryException;
import com.viller.gespatiens.access.users.application.PasswordSetupLinkSender;
import com.viller.gespatiens.access.users.domain.Language;
import com.viller.gespatiens.access.users.domain.User;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

// Adaptador: envía el enlace por correo con el servidor SMTP configurado en spring.mail.*
public class MailPasswordSetupLinkSender implements PasswordSetupLinkSender {

    private final JavaMailSender mailSender;
    private final PasswordSetupLinkFactory links;
    private final String from;

    public MailPasswordSetupLinkSender(JavaMailSender mailSender, PasswordSetupLinkFactory links, String from) {
        this.mailSender = mailSender;
        this.links = links;
        this.from = from;
    }

    @Override
    public void send(User user, String token, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        boolean english = user.getLanguage() == Language.EN;
        message.setSubject(english ? "gespatiens: create your password" : "gespatiens: crea tu contraseña");
        message.setText(body(user, links.linkFor(token), links.format(expiresAt), english));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new PasswordSetupDeliveryException(user.getEmail(), e);
        }
    }

    // Texto plano: se lee igual en cualquier cliente de correo y no lo marcan como publicidad
    private static String body(User user, String link, String expires, boolean english) {
        if (english) {
            return """
                    Hello %s,

                    You have an account in gespatiens, the management application of the Los Vientos therapeutic community.
                    To sign in for the first time, create your password with this link:

                    %s

                    The link works only once and expires on %s. If it has expired, ask the administration for a new one.
                    If you were not expecting this email, you can ignore it.
                    """.formatted(user.getName(), link, expires);
        }
        return """
                Hola, %s:

                Tienes una cuenta en gespatiens, la aplicación de gestión de la comunidad terapéutica Los Vientos.
                Para entrar por primera vez, crea tu contraseña con este enlace:

                %s

                El enlace solo sirve una vez y caduca el %s. Si ha caducado, pide otro a la administración.
                Si no esperabas este correo, puedes ignorarlo.
                """.formatted(user.getName(), link, expires);
    }
}
