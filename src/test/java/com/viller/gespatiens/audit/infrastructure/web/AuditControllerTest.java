package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.audit.application.AuditDirectory;
import com.viller.gespatiens.audit.application.SearchAuditLogUseCase;
import com.viller.gespatiens.audit.domain.AuditAction;
import com.viller.gespatiens.audit.domain.AuditEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchAuditLogUseCase searchAuditLog;
    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administrationSeesWhoOpenedARecord() throws Exception {
        UUID deleted = UUID.randomUUID();
        when(searchAuditLog.execute(LUCIA, null, LocalDate.of(2026, 10, 1), null, 0)).thenReturn(
                new SearchAuditLogUseCase.Result(List.of(
                        new SearchAuditLogUseCase.Line(
                                AuditEntry.record(Instant.parse("2026-10-21T09:00:00Z"), ANA, AuditAction.EDIT_REFERRAL, LUCIA, null),
                                "Ana", new AuditDirectory.Resident(LUCIA, 5, "Lucía", "Martínez")),
                        new SearchAuditLogUseCase.Line(
                                AuditEntry.record(Instant.parse("2026-10-20T09:00:00Z"), ANA, AuditAction.EDIT_RECORD, deleted, null),
                                "Ana", null)),
                        0, 1, 2));

        mockMvc.perform(get("/api/audit").param("patientId", LUCIA.toString()).param("from", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(2))
                .andExpect(jsonPath("$.entries[0].action").value("EDIT_REFERRAL"))
                .andExpect(jsonPath("$.entries[0].actor.name").value("Ana"))
                .andExpect(jsonPath("$.entries[0].resident.number").value(5))
                // Residente borrado: queda el id, sin nombre
                .andExpect(jsonPath("$.entries[1].resident.id").value(deleted.toString()))
                .andExpect(jsonPath("$.entries[1].resident.name").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "MEDICAL_TEAM")
    void onlyAdministrationSeesTheLog() throws Exception {
        mockMvc.perform(get("/api/audit")).andExpect(status().isForbidden());
        verifyNoInteractions(searchAuditLog);
    }
}
