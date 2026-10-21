package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.patients.application.ReferralUseCase;
import com.viller.gespatiens.patients.domain.Referral;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReferralController.class)
@Import(SecurityConfig.class)
class ReferralControllerTest {

    private static final UUID PATIENT = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");
    private static final String BODY = "{\"referringInstitution\":\"UCA Torrent\",\"courtOrdered\":true,"
            + "\"court\":\"Juzgado nº 3\",\"measureEndDate\":\"2027-04-01\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferralUseCase referralUseCase;
    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    private Referral referral() {
        return Referral.reconstitute(PATIENT, "UCA Torrent", null, null, true, "Juzgado nº 3",
                LocalDate.of(2027, 4, 1), null, Instant.parse("2026-10-21T10:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void anyEmployeeSeesTheReferral() throws Exception {
        when(referralUseCase.get(PATIENT)).thenReturn(referral());

        mockMvc.perform(get("/api/patients/{id}/referral", PATIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referringInstitution").value("UCA Torrent"))
                .andExpect(jsonPath("$.courtOrdered").value(true))
                .andExpect(jsonPath("$.measureEndDate").value("2027-04-01"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administrationCompletesIt() throws Exception {
        when(referralUseCase.update(PATIENT, "UCA Torrent", null, null, true, "Juzgado nº 3",
                LocalDate.of(2027, 4, 1), null)).thenReturn(referral());

        mockMvc.perform(put("/api/patients/{id}/referral", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.court").value("Juzgado nº 3"));
    }

    @Test
    @WithMockUser(roles = "PSYCHOLOGIST")
    void otherEmployeesCannotChangeIt() throws Exception {
        mockMvc.perform(put("/api/patients/{id}/referral", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(referralUseCase);
    }
}
