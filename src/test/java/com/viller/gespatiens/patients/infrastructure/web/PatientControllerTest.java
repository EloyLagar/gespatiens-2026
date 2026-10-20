package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.patients.application.AdmitPatientUseCase;
import com.viller.gespatiens.patients.application.DischargePatientUseCase;
import com.viller.gespatiens.patients.application.GetPatientUseCase;
import com.viller.gespatiens.patients.application.ListPatientsUseCase;
import com.viller.gespatiens.patients.application.ReadmitPatientUseCase;
import com.viller.gespatiens.patients.domain.CenterFullException;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyActiveException;
import com.viller.gespatiens.patients.domain.PatientAlreadyDischargedException;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientStatus;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private static final UUID PATIENT_ID = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdmitPatientUseCase admitPatientUseCase;

    @MockitoBean
    private DischargePatientUseCase dischargePatientUseCase;

    @MockitoBean
    private ReadmitPatientUseCase readmitPatientUseCase;

    @MockitoBean
    private GetPatientUseCase getPatientUseCase;

    @MockitoBean
    private ListPatientsUseCase listPatientsUseCase;

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

    private static Patient dischargedPatient() {
        PatientDetails details = new PatientDetails("Juan", "García López", new Dni("12345678Z"),
                LocalDate.of(1990, 5, 1), "C/ Mayor 1, Valencia", "600111222",
                null, null, null, null, null);
        return Patient.reconstitute(PATIENT_ID, details, null, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 6, 30));
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void nonAdministratorCannotDischarge() throws Exception {
        mockMvc.perform(post("/api/patients/{id}/discharge", PATIENT_ID))
                .andExpect(status().isForbidden());
        verifyNoInteractions(dischargePatientUseCase);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorDischargesPatientWithGivenDate() throws Exception {
        when(dischargePatientUseCase.execute(PATIENT_ID, LocalDate.of(2026, 6, 30))).thenReturn(dischargedPatient());

        mockMvc.perform(post("/api/patients/{id}/discharge", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"exitDate\":\"2026-06-30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.exitDate").value("2026-06-30"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void dischargeWithoutBodyUsesToday() throws Exception {
        when(dischargePatientUseCase.execute(PATIENT_ID, LocalDate.now())).thenReturn(dischargedPatient());

        mockMvc.perform(post("/api/patients/{id}/discharge", PATIENT_ID))
                .andExpect(status().isOk());
        verify(dischargePatientUseCase).execute(PATIENT_ID, LocalDate.now());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void unknownPatientReturnsNotFound() throws Exception {
        when(dischargePatientUseCase.execute(eq(PATIENT_ID), any())).thenThrow(new PatientNotFoundException(PATIENT_ID));

        mockMvc.perform(post("/api/patients/{id}/discharge", PATIENT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void alreadyDischargedPatientReturnsConflict() throws Exception {
        when(dischargePatientUseCase.execute(eq(PATIENT_ID), any()))
                .thenThrow(new PatientAlreadyDischargedException(PATIENT_ID));

        mockMvc.perform(post("/api/patients/{id}/discharge", PATIENT_ID))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void anyEmployeeReadmitsPatient() throws Exception {
        PatientDetails details = new PatientDetails("Juan", "García López", new Dni("12345678Z"),
                LocalDate.of(1990, 5, 1), "C/ Mayor 1, Valencia", "600111222",
                null, null, null, null, null);
        Patient readmitted = Patient.reconstitute(PATIENT_ID, details, 2, LocalDate.of(2026, 9, 1), null);
        when(readmitPatientUseCase.execute(PATIENT_ID, LocalDate.of(2026, 9, 1))).thenReturn(readmitted);

        mockMvc.perform(post("/api/patients/{id}/readmit", PATIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"entryDate\":\"2026-09-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.entryDate").value("2026-09-01"))
                .andExpect(jsonPath("$.exitDate").doesNotExist());
    }

    @Test
    @WithMockUser
    void readmitWithoutBodyUsesToday() throws Exception {
        when(readmitPatientUseCase.execute(PATIENT_ID, LocalDate.now()))
                .thenThrow(new PatientAlreadyActiveException(PATIENT_ID));

        mockMvc.perform(post("/api/patients/{id}/readmit", PATIENT_ID))
                .andExpect(status().isConflict());
        verify(readmitPatientUseCase).execute(PATIENT_ID, LocalDate.now());
    }

    @Test
    @WithMockUser
    void readmitWhenCenterIsFullReturnsConflict() throws Exception {
        when(readmitPatientUseCase.execute(eq(PATIENT_ID), any())).thenThrow(new CenterFullException());

        mockMvc.perform(post("/api/patients/{id}/readmit", PATIENT_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void anonymousCannotReadmit() throws Exception {
        mockMvc.perform(post("/api/patients/{id}/readmit", PATIENT_ID))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(readmitPatientUseCase);
    }

    @Test
    @WithMockUser
    void listsActivePatientsByDefault() throws Exception {
        when(listPatientsUseCase.execute(PatientStatus.ACTIVE)).thenReturn(List.of());

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(listPatientsUseCase).execute(PatientStatus.ACTIVE);
    }

    @Test
    @WithMockUser
    void listsFormerPatients() throws Exception {
        when(listPatientsUseCase.execute(PatientStatus.FORMER)).thenReturn(List.of(dischargedPatient()));

        mockMvc.perform(get("/api/patients").param("status", "FORMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[0].exitDate").value("2026-06-30"));
    }

    @Test
    @WithMockUser
    void unknownStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/patients").param("status", "OTRO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor no válido para el parámetro status: OTRO"));
        verifyNoInteractions(listPatientsUseCase);
    }

    @Test
    @WithMockUser
    void getsPatientById() throws Exception {
        when(getPatientUseCase.execute(PATIENT_ID)).thenReturn(dischargedPatient());

        mockMvc.perform(get("/api/patients/{id}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Juan"));
    }

    @Test
    @WithMockUser
    void getUnknownPatientReturnsNotFound() throws Exception {
        when(getPatientUseCase.execute(PATIENT_ID)).thenThrow(new PatientNotFoundException(PATIENT_ID));

        mockMvc.perform(get("/api/patients/{id}", PATIENT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void malformedIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/patients/{id}", "no-es-un-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(getPatientUseCase);
    }

    @Test
    void anonymousCannotListPatients() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }
}
