package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
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
                new SearchAccessLogUseCase.Line(AccessEntry.record(at, "ana@vientos.es", ANA, AccessOutcome.LOCKED,
                        "10.0.0.5", "Firefox"), "Ana"),
                new SearchAccessLogUseCase.Line(AccessEntry.record(at, "nadie@vientos.es", null,
                        AccessOutcome.FAILURE, null, null), null)), 0, 1, 2));

        mockMvc.perform(get("/api/audit/access").param("userId", ANA.toString())
                        .param("outcome", "FAILURE", "LOCKED").param("from", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(2))
                .andExpect(jsonPath("$.entries[0].outcome").value("LOCKED"))
                .andExpect(jsonPath("$.entries[0].employee.name").value("Ana"))
                .andExpect(jsonPath("$.entries[0].ipAddress").value("10.0.0.5"))
                .andExpect(jsonPath("$.entries[0].userAgent").value("Firefox"))
                .andExpect(jsonPath("$.entries[1].email").value("nadie@vientos.es"))
                .andExpect(jsonPath("$.entries[1].employee").doesNotExist());
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
