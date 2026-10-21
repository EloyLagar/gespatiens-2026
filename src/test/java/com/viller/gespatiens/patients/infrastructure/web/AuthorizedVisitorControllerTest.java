package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.patients.application.AuthorizedVisitorsUseCase;
import com.viller.gespatiens.patients.domain.AuthorizedVisitor;
import com.viller.gespatiens.patients.domain.AuthorizedVisitorNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthorizedVisitorController.class)
@Import(SecurityConfig.class)
class AuthorizedVisitorControllerTest {

    private static final UUID PATIENT = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");
    private static final String BODY = "{\"name\":\"Rosa\",\"relationship\":\"Madre\",\"phoneNumber\":\"600000001\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizedVisitorsUseCase visitorsUseCase;
    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    // Todo el personal las consulta: es quien deja pasar a las visitas
    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void anyEmployeeSeesWhoMayVisit() throws Exception {
        when(visitorsUseCase.list(PATIENT)).thenReturn(
                List.of(AuthorizedVisitor.authorize(PATIENT, "Rosa", "Madre", "600000001")));

        mockMvc.perform(get("/api/patients/{id}/visitors", PATIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rosa"))
                .andExpect(jsonPath("$[0].relationship").value("Madre"))
                .andExpect(jsonPath("$[0].phoneNumber").value("600000001"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administrationAuthorizesAVisitor() throws Exception {
        when(visitorsUseCase.authorize(PATIENT, "Rosa", "Madre", "600000001")).thenReturn(
                AuthorizedVisitor.authorize(PATIENT, "Rosa", "Madre", "600000001"));

        mockMvc.perform(post("/api/patients/{id}/visitors", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rosa"));
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void onlyAdministrationManagesThem() throws Exception {
        mockMvc.perform(post("/api/patients/{id}/visitors", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/patients/{id}/visitors/{v}", PATIENT, UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(visitorsUseCase);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void anUnknownVisitorIsNotFound() throws Exception {
        UUID visitor = UUID.randomUUID();
        doThrow(new AuthorizedVisitorNotFoundException(visitor)).when(visitorsUseCase).revoke(PATIENT, visitor);

        mockMvc.perform(delete("/api/patients/{id}/visitors/{v}", PATIENT, visitor))
                .andExpect(status().isNotFound());
    }
}
