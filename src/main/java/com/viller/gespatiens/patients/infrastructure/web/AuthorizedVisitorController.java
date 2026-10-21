package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.application.AuthorizedVisitorsUseCase;
import com.viller.gespatiens.patients.domain.AuthorizedVisitor;
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

import java.util.List;
import java.util.UUID;

// Las personas autorizadas a visitar a un residente. Las consulta todo el personal (para saber a quién dejar
// pasar) y las gestiona la administración (SecurityConfig)
@RestController
@RequestMapping("/api/patients/{patientId}/visitors")
public class AuthorizedVisitorController {

    private final AuthorizedVisitorsUseCase visitorsUseCase;

    public AuthorizedVisitorController(AuthorizedVisitorsUseCase visitorsUseCase) {
        this.visitorsUseCase = visitorsUseCase;
    }

    @GetMapping
    public List<VisitorResponse> list(@PathVariable UUID patientId) {
        return visitorsUseCase.list(patientId).stream().map(VisitorResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitorResponse authorize(@PathVariable UUID patientId, @RequestBody VisitorRequest request) {
        return VisitorResponse.from(visitorsUseCase.authorize(patientId, request.name(), request.relationship(),
                request.phoneNumber()));
    }

    @PutMapping("/{visitorId}")
    public VisitorResponse update(@PathVariable UUID patientId, @PathVariable UUID visitorId,
                                  @RequestBody VisitorRequest request) {
        return VisitorResponse.from(visitorsUseCase.update(patientId, visitorId, request.name(),
                request.relationship(), request.phoneNumber()));
    }

    @DeleteMapping("/{visitorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID patientId, @PathVariable UUID visitorId) {
        visitorsUseCase.revoke(patientId, visitorId);
    }

    public record VisitorRequest(String name, String relationship, String phoneNumber) {
    }

    public record VisitorResponse(UUID id, String name, String relationship, String phoneNumber) {
        static VisitorResponse from(AuthorizedVisitor visitor) {
            return new VisitorResponse(visitor.getId(), visitor.getName(), visitor.getRelationship(),
                    visitor.getPhoneNumber());
        }
    }
}
