package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.application.ReferralUseCase;
import com.viller.gespatiens.patients.domain.Referral;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// La derivación de un residente (memoria final, 5.2). La consulta todo el personal y la completa la
// administración (SecurityConfig: todo lo que no es GET en /api/patients/** es de la administración)
@RestController
@RequestMapping("/api/patients/{patientId}/referral")
public class ReferralController {

    private final ReferralUseCase referralUseCase;

    public ReferralController(ReferralUseCase referralUseCase) {
        this.referralUseCase = referralUseCase;
    }

    @GetMapping
    public ReferralResponse get(@PathVariable UUID patientId) {
        return ReferralResponse.from(referralUseCase.get(patientId));
    }

    @PutMapping
    public ReferralResponse update(@PathVariable UUID patientId, @RequestBody ReferralRequest request) {
        return ReferralResponse.from(referralUseCase.update(patientId, request.referringInstitution(),
                request.contactName(), request.contactPhone(), request.courtOrdered(), request.court(),
                request.measureEndDate(), request.notes()));
    }

    public record ReferralRequest(String referringInstitution, String contactName, String contactPhone,
                                  boolean courtOrdered, String court, LocalDate measureEndDate, String notes) {
    }

    public record ReferralResponse(String referringInstitution, String contactName, String contactPhone,
                                   boolean courtOrdered, String court, LocalDate measureEndDate, String notes,
                                   Instant updatedAt) {
        static ReferralResponse from(Referral r) {
            return new ReferralResponse(r.getReferringInstitution(), r.getContactName(), r.getContactPhone(),
                    r.isCourtOrdered(), r.getCourt(), r.getMeasureEndDate(), r.getNotes(), r.getUpdatedAt());
        }
    }
}
