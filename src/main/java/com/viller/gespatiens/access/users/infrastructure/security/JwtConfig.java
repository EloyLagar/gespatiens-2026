package com.viller.gespatiens.access.users.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Tokens firmados con HMAC-SHA256 y un secreto compartido: la misma aplicación los emite y los valida,
// así que no hace falta un servidor de identidad ni pares de claves pública/privada
@Configuration
public class JwtConfig {

    // HS256 exige una clave de al menos 256 bits
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public JwtConfig(@Value("${gespatiens.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("La variable de entorno JWT_SECRET debe tener al menos "
                    + MIN_SECRET_BYTES + " caracteres");
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
