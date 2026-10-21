package com.viller.gespatiens.patients.infrastructure;

import com.viller.gespatiens.patients.domain.Referral;
import com.viller.gespatiens.patients.domain.ReferralRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ReferralRepositoryJpaAdapter implements ReferralRepository {

    private final ReferralJpaRepository jpaRepository;

    public ReferralRepositoryJpaAdapter(ReferralJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Referral save(Referral referral) {
        ReferralJpaEntity entity = new ReferralJpaEntity();
        entity.setPatientId(referral.getPatientId());
        entity.setReferringInstitution(referral.getReferringInstitution());
        entity.setContactName(referral.getContactName());
        entity.setContactPhone(referral.getContactPhone());
        entity.setCourtOrdered(referral.isCourtOrdered());
        entity.setCourt(referral.getCourt());
        entity.setMeasureEndDate(referral.getMeasureEndDate());
        entity.setNotes(referral.getNotes());
        entity.setUpdatedAt(referral.getUpdatedAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Referral> findByPatient(UUID patientId) {
        return jpaRepository.findById(patientId).map(ReferralRepositoryJpaAdapter::toDomain);
    }

    private static Referral toDomain(ReferralJpaEntity e) {
        return Referral.reconstitute(e.getPatientId(), e.getReferringInstitution(), e.getContactName(),
                e.getContactPhone(), e.isCourtOrdered(), e.getCourt(), e.getMeasureEndDate(), e.getNotes(),
                e.getUpdatedAt());
    }
}
