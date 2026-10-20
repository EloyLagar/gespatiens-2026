package com.viller.gespatiens.interventions.infrastructure;

import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.ResidentDirectory;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: los residentes del módulo patients vistos desde las intervenciones.
// No se llama PatientsResidentDirectory para no chocar con el bean del mismo nombre del diario
@Component
public class InterventionsResidentDirectory implements ResidentDirectory {

    private final PatientRepository patientRepository;

    public InterventionsResidentDirectory(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Optional<Resident> find(UUID patientId) {
        return patientRepository.findById(patientId).map(InterventionsResidentDirectory::toResident);
    }

    @Override
    public List<Resident> findActive() {
        return patientRepository.findByStatus(PatientStatus.ACTIVE).stream()
                .map(InterventionsResidentDirectory::toResident)
                .sorted(Comparator.comparing(Resident::number, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static Resident toResident(Patient p) {
        return new Resident(p.getId(), p.getNumber(), p.getDetails().name(), p.getDetails().surname(), p.isActive());
    }
}
