package com.viller.gespatiens.patients.domain;

import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository {
    Referral save(Referral referral);
    Optional<Referral> findByPatient(UUID patientId);
}
