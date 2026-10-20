package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.diary.application.DeleteActivityUseCase;
import com.viller.gespatiens.diary.application.DeleteOutingUseCase;
import com.viller.gespatiens.diary.application.DeleteReductionUseCase;
import com.viller.gespatiens.diary.application.DiaryDayView;
import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.application.DiaryDayView.AttendanceRow;
import com.viller.gespatiens.diary.application.DiaryDayView.EvaluationAverages;
import com.viller.gespatiens.diary.application.DiaryDayView.OutingView;
import com.viller.gespatiens.diary.application.DiaryDayView.ReductionView;
import com.viller.gespatiens.diary.application.GetDiaryDayUseCase;
import com.viller.gespatiens.diary.application.RecordAttendanceUseCase;
import com.viller.gespatiens.diary.application.RecordEvaluationsUseCase;
import com.viller.gespatiens.diary.application.RecordOutingUseCase;
import com.viller.gespatiens.diary.application.RecordReductionUseCase;
import com.viller.gespatiens.diary.application.RecordShiftUseCase;
import com.viller.gespatiens.diary.application.RescheduleActivityUseCase;
import com.viller.gespatiens.diary.application.ScheduleActivityUseCase;
import com.viller.gespatiens.diary.application.UpdateOutingUseCase;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiaryController.class)
@Import(SecurityConfig.class)
class DiaryControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID PATIENT = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");
    private static final EvaluationAverages NO_AVERAGES =
            new EvaluationAverages(Optional.empty(), Optional.empty(), Optional.empty());
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
    @MockitoBean
    private ScheduleActivityUseCase scheduleActivityUseCase;
    @MockitoBean
    private RescheduleActivityUseCase rescheduleActivityUseCase;
    @MockitoBean
    private DeleteActivityUseCase deleteActivityUseCase;
    @MockitoBean
    private RecordAttendanceUseCase recordAttendanceUseCase;
    @MockitoBean
    private RecordEvaluationsUseCase recordEvaluationsUseCase;
    @MockitoBean
    private RecordOutingUseCase recordOutingUseCase;
    @MockitoBean
    private UpdateOutingUseCase updateOutingUseCase;
    @MockitoBean
    private DeleteOutingUseCase deleteOutingUseCase;

    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void anyEmployeeReadsTheDiaryOfADay() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 20);
        when(getDiaryDayUseCase.execute(day)).thenReturn(new DiaryDayView(day, List.of(), List.of(), List.of(), List.of()));

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

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void anyEmployeeSchedulesAnActivity() throws Exception {
        Resident lucia = new Resident(PATIENT, 5, "Lucía", "Martínez", true);
        UUID id = UUID.randomUUID();
        when(scheduleActivityUseCase.execute(LocalDate.of(2026, 10, 20), LocalTime.of(10, 30),
                ActivityType.OTHER, "Taller de cerámica"))
                .thenReturn(new ActivityView(id, LocalTime.of(10, 30), ActivityType.OTHER,
                        Optional.of("Taller de cerámica"),
                        List.of(new AttendanceRow(lucia, Optional.empty(), true, Optional.empty(), Optional.empty())),
                        NO_AVERAGES));

        mockMvc.perform(post("/api/diary/2026-10-20/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"10:30\",\"type\":\"OTHER\",\"title\":\"Taller de cerámica\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.startTime").value("10:30:00"))
                .andExpect(jsonPath("$.attendance[0].reduced").value(true))
                .andExpect(jsonPath("$.attendance[0].status").doesNotExist());
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void attendanceIsSentAsResidentToStatus() throws Exception {
        UUID activity = UUID.randomUUID();
        when(recordAttendanceUseCase.execute(activity, Map.of(PATIENT, AttendanceStatus.EXCUSED_ABSENCE)))
                .thenReturn(new ActivityView(activity, LocalTime.of(17, 0), ActivityType.WALK, Optional.empty(), List.of(),
                        NO_AVERAGES));

        mockMvc.perform(put("/api/diary/activities/{id}/attendance", activity)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendance\":{\"" + PATIENT + "\":\"EXCUSED_ABSENCE\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WALK"));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void educatorsRecordEvaluationsAndGetTheAverages() throws Exception {
        Resident lucia = new Resident(PATIENT, 5, "Lucía", "Martínez", true);
        UUID activity = UUID.randomUUID();
        Evaluation evaluation = new Evaluation(new BigDecimal("7.5"), new BigDecimal("9"), null);
        when(recordEvaluationsUseCase.execute(activity, Map.of(PATIENT, evaluation)))
                .thenReturn(new ActivityView(activity, LocalTime.of(10, 0), ActivityType.LIFE_SKILLS, Optional.empty(),
                        List.of(new AttendanceRow(lucia, Optional.of(AttendanceStatus.ATTENDED), false, Optional.empty(),
                                Optional.of(evaluation))),
                        new EvaluationAverages(Optional.of(new BigDecimal("7.5")), Optional.of(new BigDecimal("9.0")),
                                Optional.empty())));

        mockMvc.perform(put("/api/diary/activities/{id}/evaluations", activity)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluations\":{\"" + PATIENT + "\":{\"mark\":7.5,\"satisfaction\":9,\"utility\":null}}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance[0].evaluation.mark").value(7.5))
                .andExpect(jsonPath("$.averages.satisfaction").value(9.0));
    }

    @Test
    @WithMockUser(username = ME, roles = "ADMINISTRATOR")
    void anEmptyEvaluationIsLeftOutAndAnOutOfRangeOneIsBadRequest() throws Exception {
        UUID activity = UUID.randomUUID();
        when(recordEvaluationsUseCase.execute(activity, Map.of()))
                .thenReturn(new ActivityView(activity, LocalTime.of(10, 0), ActivityType.LIFE_SKILLS, Optional.empty(),
                        List.of(), NO_AVERAGES));

        mockMvc.perform(put("/api/diary/activities/{id}/evaluations", activity)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluations\":{\"" + PATIENT + "\":{\"mark\":null}}}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/diary/activities/{id}/evaluations", activity)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluations\":{\"" + PATIENT + "\":{\"mark\":11}}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void otherSpecialitiesCannotRecordEvaluations() throws Exception {
        mockMvc.perform(put("/api/diary/activities/{id}/evaluations", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluations\":{}}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recordEvaluationsUseCase);
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void anyEmployeeRecordsAnOutingAndItsReturn() throws Exception {
        Resident lucia = new Resident(PATIENT, 5, "Lucía", "Martínez", true);
        UUID outing = UUID.randomUUID();
        when(recordOutingUseCase.execute(LocalDate.of(2026, 10, 20), PATIENT, LocalTime.of(11, 0), null))
                .thenReturn(new OutingView(outing, lucia, LocalTime.of(11, 0), Optional.empty(), Optional.empty()));
        when(updateOutingUseCase.execute(outing, LocalTime.of(11, 0), LocalTime.of(19, 0), null))
                .thenReturn(new OutingView(outing, lucia, LocalTime.of(11, 0), Optional.of(LocalTime.of(19, 0)),
                        Optional.empty()));

        mockMvc.perform(post("/api/diary/2026-10-20/outings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + PATIENT + "\",\"leftAt\":\"11:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.returnedAt").doesNotExist());
        mockMvc.perform(put("/api/diary/outings/{id}", outing)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leftAt\":\"11:00\",\"returnedAt\":\"19:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").value("19:00:00"));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void deletingAnUnknownActivityIsNotFound() throws Exception {
        UUID activity = UUID.randomUUID();
        doThrow(new DiaryEntryNotFoundException("actividad", activity)).when(deleteActivityUseCase).execute(activity);

        mockMvc.perform(delete("/api/diary/activities/{id}", activity)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void unknownActivityTypeIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/diary/2026-10-20/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"10:30\",\"type\":\"KARAOKE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(scheduleActivityUseCase);
    }
}
