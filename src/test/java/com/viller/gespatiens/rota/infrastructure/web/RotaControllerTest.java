package com.viller.gespatiens.rota.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.rota.application.GetRotaUseCase;
import com.viller.gespatiens.rota.application.PlanRotaUseCase;
import com.viller.gespatiens.rota.application.RotaView;
import com.viller.gespatiens.rota.application.SwapView;
import com.viller.gespatiens.rota.application.SwapsUseCase;
import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.ShiftPart;
import com.viller.gespatiens.rota.domain.ShiftSwap;
import com.viller.gespatiens.rota.domain.SwapNotAllowedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RotaController.class)
@Import(SecurityConfig.class)
class RotaControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID ME_ID = UUID.fromString(ME);
    private static final UUID ANA_ID = UUID.fromString("7f3c2a10-0000-4000-8000-0000000000bb");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 25);

    private final Employee pablo = new Employee(ME_ID, "Pablo", "SOCIAL_EDUCATOR");
    private final Employee ana = new Employee(ANA_ID, "Ana", "PSYCHOLOGIST");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetRotaUseCase getRotaUseCase;
    @MockitoBean
    private PlanRotaUseCase planRotaUseCase;
    @MockitoBean
    private SwapsUseCase swapsUseCase;

    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    private SwapView swap(PlannedShift mine) {
        ShiftSwap s = ShiftSwap.propose(mine, ME_ID, ANA_ID, null, Instant.parse("2026-10-21T10:00:00Z"),
                LocalDate.of(2026, 10, 21));
        return new SwapView(s, Optional.of(pablo), Optional.of(ana), Optional.of(mine), Optional.empty());
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void anyEmployeeSeesTheMonth() throws Exception {
        PlannedShift shift = PlannedShift.plan(DAY, ShiftPart.MORNING, ME_ID);
        when(getRotaUseCase.execute(YearMonth.of(2026, 10)))
                .thenReturn(new RotaView(YearMonth.of(2026, 10), List.of(pablo, ana), List.of(shift)));

        mockMvc.perform(get("/api/rota").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.employees[1].name").value("Ana"))
                .andExpect(jsonPath("$.shifts[0].date").value("2026-10-25"))
                .andExpect(jsonPath("$.shifts[0].part").value("MORNING"));
    }

    // Planificar es solo de la administración
    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void othersCannotPlan() throws Exception {
        mockMvc.perform(put("/api/rota/shifts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-25\",\"part\":\"MORNING\",\"employeeId\":\"" + ME + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/rota/shifts/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rota/copy-month").contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"2026-09\",\"to\":\"2026-10\"}")).andExpect(status().isForbidden());

        verifyNoInteractions(planRotaUseCase);
    }

    @Test
    @WithMockUser(username = ME, roles = "ADMINISTRATOR")
    void administrationPlans() throws Exception {
        when(planRotaUseCase.assign(DAY, ShiftPart.NIGHT, ANA_ID)).thenReturn(PlannedShift.plan(DAY, ShiftPart.NIGHT, ANA_ID));
        when(planRotaUseCase.fill(ANA_ID, DAY, DAY.plusDays(6), Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                Set.of(ShiftPart.MORNING))).thenReturn(2);
        when(planRotaUseCase.copyMonth(YearMonth.of(2026, 9), YearMonth.of(2026, 10))).thenReturn(40);

        mockMvc.perform(put("/api/rota/shifts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-25\",\"part\":\"NIGHT\",\"employeeId\":\"" + ANA_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(ANA_ID.toString()));
        mockMvc.perform(post("/api/rota/fill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"" + ANA_ID + "\",\"from\":\"2026-10-25\",\"to\":\"2026-10-31\","
                                + "\"weekdays\":[\"MONDAY\",\"FRIDAY\"],\"parts\":[\"MORNING\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(2));
        mockMvc.perform(post("/api/rota/copy-month").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2026-09\",\"to\":\"2026-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(40));
        mockMvc.perform(delete("/api/rota/shifts/{id}", UUID.randomUUID())).andExpect(status().isNoContent());
    }

    // Los cambios de turno los acuerdan los trabajadores, sin pasar por la administración
    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void employeesProposeAndListTheirSwaps() throws Exception {
        PlannedShift mine = PlannedShift.plan(DAY, ShiftPart.MORNING, ME_ID);
        SwapView view = swap(mine);
        when(swapsUseCase.propose(ME_ID, mine.getId(), ANA_ID, null)).thenReturn(view);
        when(swapsUseCase.mine(ME_ID)).thenReturn(List.of(view));

        mockMvc.perform(post("/api/rota/swaps").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"myShiftId\":\"" + mine.getId() + "\",\"partnerId\":\"" + ANA_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.partner.name").value("Ana"))
                .andExpect(jsonPath("$.partnerShift").doesNotExist());
        mockMvc.perform(get("/api/rota/swaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requesterShift.part").value("MORNING"));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void aSwapThatCannotBeDoneIsAConflict() throws Exception {
        UUID id = UUID.randomUUID();
        when(swapsUseCase.accept(id, ME_ID)).thenThrow(new SwapNotAllowedException("El cuadrante ha cambiado"));

        mockMvc.perform(post("/api/rota/swaps/{id}/accept", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El cuadrante ha cambiado"));
    }

    @Test
    void anonymousCannotSeeTheRota() throws Exception {
        mockMvc.perform(get("/api/rota").param("month", "2026-10")).andExpect(status().isUnauthorized());
    }
}
