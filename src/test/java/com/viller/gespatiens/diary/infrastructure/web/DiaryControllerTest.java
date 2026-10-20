package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.diary.application.DeleteReductionUseCase;
import com.viller.gespatiens.diary.application.DiaryDayView;
import com.viller.gespatiens.diary.application.DiaryDayView.ReductionView;
import com.viller.gespatiens.diary.application.GetDiaryDayUseCase;
import com.viller.gespatiens.diary.application.RecordReductionUseCase;
import com.viller.gespatiens.diary.application.RecordShiftUseCase;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiaryController.class)
@Import(SecurityConfig.class)
class DiaryControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID PATIENT = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");
    private static final String REDUCTION = "{\"partial\":\"No puede cargar peso\",\"total\":false,\"sport\":true}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDiaryDayUseCase getDiaryDayUseCase;
    @MockitoBean
    private RecordShiftUseCase recordShiftUseCase;
    @MockitoBean
    private RecordReductionUseCase recordReductionUseCase;
    @MockitoBean
    private DeleteReductionUseCase deleteReductionUseCase;

    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void anyEmployeeReadsTheDiaryOfADay() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 20);
        when(getDiaryDayUseCase.execute(day)).thenReturn(new DiaryDayView(day, List.of(), List.of()));

        mockMvc.perform(get("/api/diary/2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-20"));
    }

    @Test
    @WithMockUser(username = ME)
    void badDateIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/diary/20-10-2026")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = ME, roles = "MEDICAL_TEAM")
    void medicalTeamRecordsAReduction() throws Exception {
        Resident lucia = new Resident(PATIENT, 5, "Lucía", "Martínez", true);
        when(recordReductionUseCase.execute(eq(LocalDate.of(2026, 10, 20)), eq(PATIENT), any(), anyBoolean(),
                anyBoolean(), eq(UUID.fromString(ME))))
                .thenReturn(new ReductionView(lucia, Optional.of("No puede cargar peso"), false, true));

        mockMvc.perform(put("/api/diary/2026-10-20/reductions/{p}", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content(REDUCTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resident.number").value(5))
                .andExpect(jsonPath("$.sport").value(true));
    }

    @Test
    @WithMockUser(username = ME, roles = "ADMINISTRATOR")
    void administrationCanAlsoRecordReductions() throws Exception {
        Resident lucia = new Resident(PATIENT, 5, "Lucía", "Martínez", true);
        when(recordReductionUseCase.execute(eq(LocalDate.of(2026, 10, 20)), eq(PATIENT), any(), anyBoolean(),
                anyBoolean(), eq(UUID.fromString(ME))))
                .thenReturn(new ReductionView(lucia, Optional.empty(), true, false));

        mockMvc.perform(put("/api/diary/2026-10-20/reductions/{p}", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content(REDUCTION))
                .andExpect(status().isOk());
    }

    // La memoria: "Los demás usuarios solo deben poder verlo"
    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void otherSpecialitiesCannotRecordOrDeleteReductions() throws Exception {
        mockMvc.perform(put("/api/diary/2026-10-20/reductions/{p}", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content(REDUCTION))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/diary/2026-10-20/reductions/{p}", PATIENT))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recordReductionUseCase, deleteReductionUseCase);
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void unknownShiftIsBadRequest() throws Exception {
        mockMvc.perform(put("/api/diary/2026-10-20/shifts/BREAKFAST")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"staffIds\":[],\"notes\":null}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recordShiftUseCase);
    }
}
