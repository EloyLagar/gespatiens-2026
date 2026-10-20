package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.User;

// Puerto: el login necesita emitir un token de acceso, pero no sabe que es un JWT.
// La implementación (JwtAccessTokenIssuer) vive en infrastructure
public interface AccessTokenIssuer {
    AccessToken issueFor(User user);
}
