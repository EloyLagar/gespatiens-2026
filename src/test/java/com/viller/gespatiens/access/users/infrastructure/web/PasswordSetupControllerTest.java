package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.PasswordSetupUseCase;
import com.viller.gespatiens.access.users.domain.InvalidPasswordSetupTokenException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sin @WithMockUser: quien abre el enlace del correo todavía no puede entrar
@WebMvcTest(PasswordSetupController.class)
@Import(SecurityConfig.class)
class PasswordSetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordSetupUseCase passwordSetupUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    void anonymousVisitorSeesOnlyNameAndEmail() throws Exception {
        when(passwordSetupUseCase.check("abc"))
                .thenReturn(User.invite("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST));

        mockMvc.perform(get("/api/auth/password-setup/abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@vientos.es"))
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void anonymousVisitorCreatesThePassword() throws Exception {
        mockMvc.perform(post("/api/auth/password-setup/abc").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"secreto123\"}"))
                .andExpect(status().isNoContent());
        verify(passwordSetupUseCase).complete("abc", "secreto123");
    }

    @Test
    void invalidLinkReturnsGone() throws Exception {
        when(passwordSetupUseCase.check("caducado")).thenThrow(new InvalidPasswordSetupTokenException());

        mockMvc.perform(get("/api/auth/password-setup/caducado"))
                .andExpect(status().isGone());
    }

    @Test
    void shortPasswordReturnsBadRequest() throws Exception {
        when(passwordSetupUseCase.complete("abc", "corta"))
                .thenThrow(new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres"));

        mockMvc.perform(post("/api/auth/password-setup/abc").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }
}
