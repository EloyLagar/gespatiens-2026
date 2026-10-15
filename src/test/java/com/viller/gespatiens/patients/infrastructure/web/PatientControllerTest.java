package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.patients.application.AdmitPatientUseCase;
import com.viller.gespatiens.patients.domain.CenterFullException;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Levanta solo la capa web + seguridad, con el caso de uso simulado (no necesita PostgreSQL)
@WebMvcTest(PatientController.class)
@Import(SecurityConfig.class)
class PatientControllerTest {

    private static final String VALID_BODY = """
            {"name":"Juan","surname":"García López","dni":"12345678Z","birthDate":"1990-05-01",
             "address":"C/ Mayor 1, Valencia","phoneNumber":"600111222","abuseSubstances":"Alcohol"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdmitPatientUseCase admitPatientUseCase;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void anyEmployeeAdmitsPatient() throws Exception {
        PatientDetails details = new PatientDetails("Juan", "García López", new Dni("12345678Z"),
                LocalDate.of(1990, 5, 1), "C/ Mayor 1, Valencia", "600111222",
                null, null, null, "Alcohol", null);
        when(admitPatientUseCase.execute(any(), any()))
                .thenReturn(Patient.admit(details, 1, LocalDate.of(2026, 10, 15)));

        mockMvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.dni").value("12345678Z"))
                .andExpect(jsonPath("$.entryDate").value("2026-10-15"));
    }

    @Test
    @WithMockUser
    void invalidDniReturnsBadRequestWithoutCallingUseCase() throws Exception {
        String body = VALID_BODY.replace("12345678Z", "12345678A");

        mockMvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La letra del DNI/NIE no es correcta"));
        verifyNoInteractions(admitPatientUseCase);
    }

    @Test
    @WithMockUser
    void badDateFormatReturnsBadRequest() throws Exception {
        String body = VALID_BODY.replace("1990-05-01", "01/05/1990");

        mockMvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void fullCenterReturnsConflict() throws Exception {
        when(admitPatientUseCase.execute(any(), any())).thenThrow(new CenterFullException());

        mockMvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }
}
