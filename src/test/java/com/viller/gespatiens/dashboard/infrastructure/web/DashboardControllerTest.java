package com.viller.gespatiens.dashboard.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.dashboard.application.DashboardQueries.ShiftCount;
import com.viller.gespatiens.dashboard.application.DashboardUseCase;
import com.viller.gespatiens.dashboard.application.DashboardView;
import com.viller.gespatiens.dashboard.application.DashboardView.WeekCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardUseCase dashboardUseCase;
    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administrationSeesTheMonth() throws Exception {
        when(dashboardUseCase.execute(YearMonth.of(2026, 10))).thenReturn(new DashboardView(YearMonth.of(2026, 10),
                27, 32, 3, 12, 40, List.of(new ShiftCount(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR", 4, 2, 3)),
                List.of(new WeekCount(LocalDate.of(2026, 9, 28), 2))));

        mockMvc.perform(get("/api/dashboard").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedPlaces").value(27))
                .andExpect(jsonPath("$.shifts[0].total").value(9))
                .andExpect(jsonPath("$.noticesByWeek[0].monday").value("2026-09-28"));
    }

    // Memoria final, 5.1: "al que solo esta [la administración] pudiese acceder"
    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void nobodyElseSeesIt() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("month", "2026-10")).andExpect(status().isForbidden());
        verifyNoInteractions(dashboardUseCase);
    }
}
