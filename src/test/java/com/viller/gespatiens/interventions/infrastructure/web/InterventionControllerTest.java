package com.viller.gespatiens.interventions.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.interventions.application.AssignTutorsUseCase;
import com.viller.gespatiens.interventions.application.DeleteInterventionUseCase;
import com.viller.gespatiens.interventions.application.EditInterventionUseCase;
import com.viller.gespatiens.interventions.application.FollowUpSummary;
import com.viller.gespatiens.interventions.application.FollowUpView;
import com.viller.gespatiens.interventions.application.GetFollowUpUseCase;
import com.viller.gespatiens.interventions.application.InterventionView;
import com.viller.gespatiens.interventions.application.ListFollowUpsUseCase;
import com.viller.gespatiens.interventions.application.RecordInterventionUseCase;
import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionNotManageableException;
import com.viller.gespatiens.interventions.domain.NotATutorException;
import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.ResidentNotFoundException;
import com.viller.gespatiens.interventions.domain.StaffMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterventionController.class)
@Import(SecurityConfig.class)
class InterventionControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID ME_ID = UUID.fromString(ME);
    private static final UUID PATIENT = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);

    private final Resident lucia = new Resident(PATIENT, 5, "Lucía", "Martínez", true);
    private final StaffMember pablo = new StaffMember(ME_ID, "Pablo", "SOCIAL_EDUCATOR", false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListFollowUpsUseCase listFollowUpsUseCase;
    @MockitoBean
    private GetFollowUpUseCase getFollowUpUseCase;
    @MockitoBean
    private AssignTutorsUseCase assignTutorsUseCase;
    @MockitoBean
    private RecordInterventionUseCase recordInterventionUseCase;
    @MockitoBean
    private EditInterventionUseCase editInterventionUseCase;
    @MockitoBean
    private DeleteInterventionUseCase deleteInterventionUseCase;

    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    private InterventionView intervention(String text) {
        return new InterventionView(Intervention.reconstitute(UUID.randomUUID(), PATIENT, ME_ID, DAY, text,
                Instant.parse("2026-10-21T10:00:00Z"), null), Optional.of(pablo), true, true);
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void anyEmployeeSeesTheListWithTutorsAndLastIntervention() throws Exception {
        when(listFollowUpsUseCase.execute(ME_ID)).thenReturn(List.of(
                new FollowUpSummary(lucia, List.of(pablo), 3, Optional.of(DAY), false)));

        mockMvc.perform(get("/api/interventions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resident.number").value(5))
                .andExpect(jsonPath("$[0].tutors[0].name").value("Pablo"))
                .andExpect(jsonPath("$[0].interventionCount").value(3))
                .andExpect(jsonPath("$[0].lastIntervention").value("2026-10-21"))
                .andExpect(jsonPath("$[0].mine").value(false));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void aTutorCanRecordWhileTheResidentIsInTheCentre() throws Exception {
        when(getFollowUpUseCase.execute(PATIENT, ME_ID)).thenReturn(
                new FollowUpView(lucia, List.of(pablo), List.of(intervention("Tutoría")), true));

        mockMvc.perform(get("/api/interventions/residents/{id}", PATIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutor").value(true))
                .andExpect(jsonPath("$.canRecord").value(true))
                .andExpect(jsonPath("$.interventions[0].text").value("Tutoría"))
                .andExpect(jsonPath("$.interventions[0].author.name").value("Pablo"));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void unknownResidentIsNotFound() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(getFollowUpUseCase.execute(unknown, ME_ID)).thenThrow(new ResidentNotFoundException(unknown));

        mockMvc.perform(get("/api/interventions/residents/{id}", unknown))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = ME, roles = "ADMINISTRATOR")
    void administrationAssignsTutors() throws Exception {
        when(assignTutorsUseCase.execute(PATIENT, Set.of(ME_ID))).thenReturn(List.of(pablo));

        mockMvc.perform(put("/api/interventions/residents/{id}/tutors", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tutorIds\":[\"" + ME + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pablo"));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void educatorsAlsoAssignTutors() throws Exception {
        when(assignTutorsUseCase.execute(PATIENT, Set.of(ME_ID))).thenReturn(List.of(pablo));

        mockMvc.perform(put("/api/interventions/residents/{id}/tutors", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tutorIds\":[\"" + ME + "\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void otherSpecialitiesCannotAssignTutors() throws Exception {
        mockMvc.perform(put("/api/interventions/residents/{id}/tutors", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tutorIds\":[\"" + ME + "\"]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(assignTutorsUseCase);
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void recordingAnInterventionReturnsItAndANonTutorIsForbidden() throws Exception {
        when(recordInterventionUseCase.execute(PATIENT, ME_ID, DAY, "Tutoría")).thenReturn(intervention("Tutoría"));
        UUID other = UUID.randomUUID();
        when(recordInterventionUseCase.execute(other, ME_ID, DAY, "Tutoría")).thenThrow(new NotATutorException());

        mockMvc.perform(post("/api/interventions/residents/{id}", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-21\",\"text\":\"Tutoría\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true));
        mockMvc.perform(post("/api/interventions/residents/{id}", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-21\",\"text\":\"Tutoría\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void editingOrDeletingSomeoneElsesInterventionIsForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        when(editInterventionUseCase.execute(id, ME_ID, DAY, "Otra cosa"))
                .thenThrow(new InterventionNotManageableException());
        doThrow(new InterventionNotManageableException()).when(deleteInterventionUseCase).execute(id, ME_ID);

        mockMvc.perform(put("/api/interventions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-21\",\"text\":\"Otra cosa\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/interventions/{id}", id))
                .andExpect(status().isForbidden());
    }
}
