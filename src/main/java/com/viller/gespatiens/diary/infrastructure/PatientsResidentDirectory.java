package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: los residentes del módulo patients vistos desde el diario
@Component
public class PatientsResidentDirectory implements ResidentDirectory {

    private final PatientRepository patientRepository;

    public PatientsResidentDirectory(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Optional<Resident> find(UUID patientId) {
        return patientRepository.findById(patientId).map(PatientsResidentDirectory::toResident);
    }

    @Override
    public List<Resident> findActive() {
        return patientRepository.findByStatus(PatientStatus.ACTIVE).stream()
                .map(PatientsResidentDirectory::toResident)
                .toList();
    }

    private static Resident toResident(Patient p) {
        return new Resident(p.getId(), p.getNumber(), p.getDetails().name(), p.getDetails().surname(), p.isActive());
    }
}
