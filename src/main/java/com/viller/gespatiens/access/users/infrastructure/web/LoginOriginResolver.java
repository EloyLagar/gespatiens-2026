package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.LoginOrigin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

// La IP y el navegador de quien intenta entrar. La IP es la que ve Spring: detrás del nginx de Docker o de un proxy
// de la red interna, la del cliente (server.forward-headers-strategy). Con anonymize (la demo publicada), la IP se
// guarda sin su última parte: sirve para ver de qué red viene, no para identificar a un visitante
@Component
public class LoginOriginResolver {

    // Un User-Agent no necesita más para saber el navegador y el sistema, y la columna tiene ese tamaño
    static final int MAX_USER_AGENT = 255;

    private final boolean anonymize;

    public LoginOriginResolver(@Value("${gespatiens.access-log.anonymize-ip}") boolean anonymize) {
        this.anonymize = anonymize;
    }

    public LoginOrigin resolve(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null && userAgent.length() > MAX_USER_AGENT) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT);
        }
        return new LoginOrigin(anonymize ? anonymize(ip) : ip, userAgent);
    }

    // IPv4: el último número a 0 (192.168.1.37 -> 192.168.1.0). IPv6: se quedan los 48 primeros bits
    static String anonymize(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            // Solo literales: getRemoteAddr nunca trae un nombre, así que no hay consulta DNS
            byte[] bytes = InetAddress.getByName(ip).getAddress();
            int keep = bytes.length == 4 ? 3 : 6;
            for (int i = keep; i < bytes.length; i++) {
                bytes[i] = 0;
            }
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
