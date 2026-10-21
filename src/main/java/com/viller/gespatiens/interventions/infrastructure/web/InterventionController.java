package com.viller.gespatiens.interventions.infrastructure.web;

import com.viller.gespatiens.interventions.application.AssignTutorsUseCase;
import com.viller.gespatiens.interventions.application.DeleteInterventionUseCase;
import com.viller.gespatiens.interventions.application.EditInterventionUseCase;
import com.viller.gespatiens.interventions.application.GetFollowUpUseCase;
import com.viller.gespatiens.interventions.application.ListFollowUpsUseCase;
import com.viller.gespatiens.interventions.application.RecordInterventionUseCase;
import com.viller.gespatiens.interventions.infrastructure.web.InterventionRequests.InterventionRequest;
import com.viller.gespatiens.interventions.infrastructure.web.InterventionRequests.TutorsRequest;
import com.viller.gespatiens.interventions.infrastructure.web.InterventionResponses.FollowUpResponse;
import com.viller.gespatiens.interventions.infrastructure.web.InterventionResponses.FollowUpSummaryResponse;
import com.viller.gespatiens.interventions.infrastructure.web.InterventionResponses.InterventionResponse;
import com.viller.gespatiens.interventions.infrastructure.web.InterventionResponses.StaffResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

// El apartado "Intervenciones". Todo el personal consulta; los tutores los asignan la administración y
// los educadores (ver SecurityConfig) y quién registra, corrige o elimina cada intervención lo decide el dominio
@RestController
@RequestMapping("/api/interventions")
public class InterventionController {

    private final ListFollowUpsUseCase listFollowUpsUseCase;
    private final GetFollowUpUseCase getFollowUpUseCase;
    private final AssignTutorsUseCase assignTutorsUseCase;
    private final RecordInterventionUseCase recordInterventionUseCase;
    private final EditInterventionUseCase editInterventionUseCase;
    private final DeleteInterventionUseCase deleteInterventionUseCase;

    public InterventionController(ListFollowUpsUseCase listFollowUpsUseCase,
                                  GetFollowUpUseCase getFollowUpUseCase,
                                  AssignTutorsUseCase assignTutorsUseCase,
                                  RecordInterventionUseCase recordInterventionUseCase,
                                  EditInterventionUseCase editInterventionUseCase,
                                  DeleteInterventionUseCase deleteInterventionUseCase) {
        this.listFollowUpsUseCase = listFollowUpsUseCase;
        this.getFollowUpUseCase = getFollowUpUseCase;
        this.assignTutorsUseCase = assignTutorsUseCase;
        this.recordInterventionUseCase = recordInterventionUseCase;
        this.editInterventionUseCase = editInterventionUseCase;
        this.deleteInterventionUseCase = deleteInterventionUseCase;
    }

    @GetMapping
    public List<FollowUpSummaryResponse> list(Principal principal) {
        return listFollowUpsUseCase.execute(currentUserId(principal)).stream()
                .map(FollowUpSummaryResponse::from)
                .toList();
    }

    @GetMapping("/residents/{patientId}")
    public FollowUpResponse get(Principal principal, @PathVariable UUID patientId) {
        return FollowUpResponse.from(getFollowUpUseCase.execute(patientId, currentUserId(principal)));
    }

    @PutMapping("/residents/{patientId}/tutors")
    public List<StaffResponse> assignTutors(@PathVariable UUID patientId, @RequestBody TutorsRequest request) {
        return StaffResponse.from(assignTutorsUseCase.execute(patientId, request.educatorId(),
                request.psychologistId()));
    }

    @PostMapping("/residents/{patientId}")
    @ResponseStatus(HttpStatus.CREATED)
    public InterventionResponse record(Principal principal, @PathVariable UUID patientId,
                                       @RequestBody InterventionRequest request) {
        return InterventionResponse.from(recordInterventionUseCase.execute(patientId, currentUserId(principal),
                request.date(), request.text()));
    }

    @PutMapping("/{id}")
    public InterventionResponse edit(Principal principal, @PathVariable UUID id,
                                     @RequestBody InterventionRequest request) {
        return InterventionResponse.from(editInterventionUseCase.execute(id, currentUserId(principal),
                request.date(), request.text()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable UUID id) {
        deleteInterventionUseCase.execute(id, currentUserId(principal));
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
