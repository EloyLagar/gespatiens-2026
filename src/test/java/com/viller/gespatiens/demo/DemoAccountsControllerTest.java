package com.viller.gespatiens.demo;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sin @WithMockUser: las cuentas se piden desde la pantalla de login, antes de entrar
@WebMvcTest(DemoAccountsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("demo")
class DemoAccountsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    void anonymousVisitorGetsOneAccountPerProfile() throws Exception {
        mockMvc.perform(get("/api/demo/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].email").value("marta.serra@vientos.es"))
                .andExpect(jsonPath("$[0].speciality").value("ADMINISTRATOR"))
                .andExpect(jsonPath("$[0].password").value(DemoData.PASSWORD));
    }
}
