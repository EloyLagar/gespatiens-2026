package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.AccessToken;
import com.viller.gespatiens.access.users.application.InvalidCredentialsException;
import com.viller.gespatiens.access.users.application.LoginBlockedException;
import com.viller.gespatiens.access.users.application.LoginOrigin;
import com.viller.gespatiens.access.users.application.LoginUseCase;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, LoginOriginResolver.class})
@TestPropertySource(properties = "gespatiens.access-log.anonymize-ip=false")
class AuthControllerTest {

    private static final String BODY = "{\"email\":\"ana@vientos.es\",\"password\":\"secreto123\"}";
    // MockMvc hace las peticiones desde 127.0.0.1
    private static final LoginOrigin ORIGIN = new LoginOrigin("127.0.0.1", "Firefox de pruebas");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    void loginReturnsBearerTokenAndUserWithoutPassword() throws Exception {
        User ana = User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash");
        when(loginUseCase.execute("ana@vientos.es", "secreto123", ORIGIN)).thenReturn(new LoginUseCase.Session(ana,
                new AccessToken("abc.def.ghi", Instant.parse("2026-10-20T23:00:00Z"))));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header(HttpHeaders.USER_AGENT, "Firefox de pruebas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("abc.def.ghi"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-20T23:00:00Z"))
                .andExpect(jsonPath("$.user.email").value("ana@vientos.es"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void wrongCredentialsReturnUnauthorized() throws Exception {
        when(loginUseCase.execute("ana@vientos.es", "secreto123", ORIGIN)).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header(HttpHeaders.USER_AGENT, "Firefox de pruebas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
    }

    @Test
    void blockedEmailReturnsTooManyRequests() throws Exception {
        Instant now = Instant.parse("2026-10-21T10:00:00Z");
        when(loginUseCase.execute("ana@vientos.es", "secreto123", ORIGIN))
                .thenThrow(new LoginBlockedException(now.plusSeconds(15 * 60), now));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header(HttpHeaders.USER_AGENT, "Firefox de pruebas"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Demasiados intentos fallidos. Vuelve a intentarlo dentro de 15 minutos"))
                .andExpect(jsonPath("$.retryAfter").value("2026-10-21T10:15:00Z"));
    }
}
