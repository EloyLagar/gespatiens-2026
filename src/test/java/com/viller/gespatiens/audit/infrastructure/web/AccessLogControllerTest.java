package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.audit.application.MyLastAccessUseCase;
import com.viller.gespatiens.audit.application.SearchAccessLogUseCase;
import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessOutcome;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccessLogController.class)
@Import(SecurityConfig.class)
class AccessLogControllerTest {

    private static final UUID ANA = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchAccessLogUseCase searchAccessLog;
    @MockitoBean
    private MyLastAccessUseCase myLastAccess;
    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administrationSeesTheFailedAttempts() throws Exception {
        Instant at = Instant.parse("2026-10-21T09:00:00Z");
        when(searchAccessLog.execute(ANA, Set.of(AccessOutcome.FAILURE, AccessOutcome.LOCKED),
                LocalDate.of(2026, 10, 1), null, 0)).thenReturn(new SearchAccessLogUseCase.Result(List.of(
                new SearchAccessLogUseCase.Line(AccessEntry.record(at, "ana@vientos.es", ANA, null,
                        AccessOutcome.LOCKED, "10.0.0.5", "Firefox"), "Ana", null),
                new SearchAccessLogUseCase.Line(AccessEntry.record(at, "nadie@vientos.es", null, null,
                        AccessOutcome.FAILURE, null, null), null, null)), 0, 1, 2));

        mockMvc.perform(get("/api/audit/access").param("userId", ANA.toString())
                        .param("outcome", "FAILURE", "LOCKED").param("from", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(2))
                .andExpect(jsonPath("$.entries[0].outcome").value("LOCKED"))
                .andExpect(jsonPath("$.entries[0].employee.name").value("Ana"))
                .andExpect(jsonPath("$.entries[0].ipAddress").value("10.0.0.5"))
                .andExpect(jsonPath("$.entries[0].userAgent").value("Firefox"))
                .andExpect(jsonPath("$.entries[1].email").value("nadie@vientos.es"))
                .andExpect(jsonPath("$.entries[1].employee").doesNotExist())
                .andExpect(jsonPath("$.entries[0].actor").doesNotExist());
    }

    // ENS op.acc: cada empleado, sea de la especialidad que sea, ve su acceso anterior al entrar
    @Test
    @WithMockUser(username = "6f1d2c1e-0000-4000-8000-000000000001", roles = "PSYCHOLOGIST")
    void everyEmployeeSeesTheirOwnPreviousAccess() throws Exception {
        UUID me = UUID.fromString("6f1d2c1e-0000-4000-8000-000000000001");
        when(myLastAccess.execute(me)).thenReturn(new MyLastAccessUseCase.Result(
                AccessEntry.record(Instant.parse("2026-10-20T07:02:00Z"), "ana@vientos.es", me, null,
                        AccessOutcome.SUCCESS, "192.168.1.37", "Firefox"), 2));

        mockMvc.perform(get("/api/audit/access/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previous.occurredAt").value("2026-10-20T07:02:00Z"))
                .andExpect(jsonPath("$.previous.ipAddress").value("192.168.1.37"))
                .andExpect(jsonPath("$.failedAttemptsSince").value(2))
                .andExpect(jsonPath("$.previous.email").doesNotExist());
    }

    @Test
    void theOwnPreviousAccessNeedsALogin() throws Exception {
        mockMvc.perform(get("/api/audit/access/mine")).andExpect(status().isUnauthorized());
    }

    // Quién entra y desde dónde solo lo ve la administración
    @Test
    @WithMockUser(roles = "PSYCHOLOGIST")
    void otherSpecialitiesCannotSeeIt() throws Exception {
        mockMvc.perform(get("/api/audit/access")).andExpect(status().isForbidden());
        verifyNoInteractions(searchAccessLog);
    }

    @Test
    void withoutTokenItIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/audit/access")).andExpect(status().isUnauthorized());
    }
}
