package com.viller.gespatiens.access.users.infrastructure.mail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// La URL de la pantalla del frontend que abre el enlace, y la fecha de caducidad tal como se lee en el centro
public class PasswordSetupLinkFactory {

    private static final ZoneId CENTER_ZONE = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter EXPIRES = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String frontendUrl;

    public PasswordSetupLinkFactory(String frontendUrl) {
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    // El token es Base64 URL-safe: va tal cual en la query, sin codificar
    public String linkFor(String token) {
        return frontendUrl + "/crear-contrasena?token=" + token;
    }

    public String format(Instant instant) {
        return EXPIRES.format(instant.atZone(CENTER_ZONE));
    }
}
