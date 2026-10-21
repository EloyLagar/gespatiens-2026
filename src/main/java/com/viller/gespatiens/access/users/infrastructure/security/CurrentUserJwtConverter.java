package com.viller.gespatiens.access.users.infrastructure.security;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// Spring Security llama a este conversor con cada token ya validado (firma y caducidad).
// Carga al empleado de la BD: si lo han borrado, el token deja de servir aunque no haya caducado,
// y su rol es siempre el actual
@Component
public class CurrentUserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public CurrentUserJwtConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = parseId(jwt.getSubject())
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new InvalidBearerTokenException("El empleado del token ya no existe"));
        // La especialidad hace de rol: ADMINISTRATOR -> ROLE_ADMINISTRATOR
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getSpeciality().name()));
        // El "name" de la autenticación es el id: es lo que reciben los endpoints /me como Principal
        return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
    }

    private static java.util.Optional<UUID> parseId(String subject) {
        try {
            return java.util.Optional.of(UUID.fromString(subject));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return java.util.Optional.empty();
        }
    }
}
