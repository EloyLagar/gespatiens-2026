package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsUseCase;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.ActivityRef;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.Cell;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.DayColumn;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.ResidentRow;
import com.viller.gespatiens.diary.application.RecordMarkUseCase;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.Resident;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonthlyEvaluationsController.class)
@Import(SecurityConfig.class)
class MonthlyEvaluationsControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID PATIENT = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthlyEvaluationsUseCase monthlyEvaluationsUseCase;
    @MockitoBean
    private RecordMarkUseCase recordMarkUseCase;
    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    // Todo el personal consulta la tabla
    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void anyEmployeeSeesTheMonthlyGrid() throws Exception {
        UUID activity = UUID.randomUUID();
        Resident juan = new Resident(PATIENT, 3, "Juan", "García", true);
        when(monthlyEvaluationsUseCase.execute(YearMonth.of(2026, 10), ActivityType.LIFE_SKILLS)).thenReturn(
                new MonthlyEvaluationsView(YearMonth.of(2026, 10), ActivityType.LIFE_SKILLS,
                        List.of(new DayColumn(LocalDate.of(2026, 10, 1),
                                List.of(new ActivityRef(activity, LocalTime.of(10, 0), Optional.empty(), 2)))),
                        List.of(new ResidentRow(juan, List.of(
                                new Cell(Optional.of(new BigDecimal("7.5")), true, Optional.of(activity)))))));

        mockMvc.perform(get("/api/diary/evaluations").param("month", "2026-10").param("type", "LIFE_SKILLS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.days[0].activities[0].version").value(2))
                .andExpect(jsonPath("$.rows[0].resident.name").value("Juan"))
                .andExpect(jsonPath("$.rows[0].cells[0].mark").value(7.5))
                .andExpect(jsonPath("$.rows[0].cells[0].activityId").value(activity.toString()));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void educatorsChangeAMarkAndGetTheNewVersion() throws Exception {
        UUID activity = UUID.randomUUID();
        when(recordMarkUseCase.execute(activity, PATIENT, new BigDecimal("8"), UUID.fromString(ME), 2L)).thenReturn(3L);

        mockMvc.perform(put("/api/diary/activities/{a}/evaluations/{p}", activity, PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mark\":8,\"version\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void otherSpecialitiesCannotChangeMarks() throws Exception {
        mockMvc.perform(put("/api/diary/activities/{a}/evaluations/{p}", UUID.randomUUID(), PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mark\":8,\"version\":2}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recordMarkUseCase);
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void aBadMonthIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/diary/evaluations").param("month", "octubre").param("type", "LIFE_SKILLS"))
                .andExpect(status().isBadRequest());
    }
}
