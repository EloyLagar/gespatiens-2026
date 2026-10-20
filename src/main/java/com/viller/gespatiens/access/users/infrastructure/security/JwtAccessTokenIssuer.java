package com.viller.gespatiens.access.users.infrastructure.security;

import com.viller.gespatiens.access.users.application.AccessToken;
import com.viller.gespatiens.access.users.application.AccessTokenIssuer;
import com.viller.gespatiens.access.users.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// Adaptador: implementa el puerto AccessTokenIssuer con un JWT firmado
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder jwtEncoder,
                                @Value("${gespatiens.jwt.expiration}") Duration expiration) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
        this.clock = Clock.systemUTC();
    }

    @Override
    public AccessToken issueFor(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        // El token solo identifica al empleado (subject = su id). Rol, nombre y email se leen
        // de la BD en cada petición (CurrentUserJwtConverter), así un cambio de la administración
        // se aplica en el acto y no cuando caduque el token
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gespatiens")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}
