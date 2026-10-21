package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.Referral;
import com.viller.gespatiens.patients.domain.ReferralRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

// La derivación de un residente (memoria final, 5.2). La consulta todo el personal y la completa la
// administración, como el resto de la ficha (SecurityConfig)
public class ReferralUseCase {

    private final PatientRepository patientRepository;
    private final ReferralRepository referralRepository;
    private final Clock clock;

    public ReferralUseCase(PatientRepository patientRepository, ReferralRepository referralRepository, Clock clock) {
        this.patientRepository = patientRepository;
        this.referralRepository = referralRepository;
        this.clock = clock;
    }

    // Sin rellenar todavía: una derivación vacía
    public Referral get(UUID patientId) {
        ensurePatient(patientId);
        return referralRepository.findByPatient(patientId)
                .orElseGet(() -> Referral.none(patientId, clock.instant()));
    }

    public Referral update(UUID patientId, String referringInstitution, String contactName, String contactPhone,
                           boolean courtOrdered, String court, LocalDate measureEndDate, String notes) {
        Referral referral = get(patientId);
        referral.update(referringInstitution, contactName, contactPhone, courtOrdered, court, measureEndDate, notes,
                clock.instant());
        return referralRepository.save(referral);
    }

    private void ensurePatient(UUID patientId) {
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }
    }
}
