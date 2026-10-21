package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.PatientStatus;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: los residentes del módulo patients vistos desde los informes.
// No se llama PatientsResidentDirectory para no chocar con el bean del mismo nombre del diario
@Component
public class ReportsResidentDirectory implements ResidentDirectory {

    private final PatientRepository patientRepository;

    public ReportsResidentDirectory(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Optional<Resident> find(UUID patientId) {
        return patientRepository.findById(patientId).map(ReportsResidentDirectory::toResident);
    }

    @Override
    public Map<UUID, Resident> findAll(Collection<UUID> patientIds) {
        // Como mucho unas decenas de residentes con informes: una consulta por residente es suficiente
        Map<UUID, Resident> residents = new HashMap<>();
        for (UUID id : patientIds) {
            find(id).ifPresent(resident -> residents.put(id, resident));
        }
        return residents;
    }

    @Override
    public List<Resident> findActive() {
        return patientRepository.findByStatus(PatientStatus.ACTIVE).stream()
                .map(ReportsResidentDirectory::toResident)
                .toList();
    }

    private static Resident toResident(Patient p) {
        PatientDetails d = p.getDetails();
        return new Resident(p.getId(), p.getNumber(), d.name(), d.surname(), d.dni().value(), d.birthDate(), d.sip(),
                p.getEntryDate(), p.getExitDate(), p.isActive());
    }
}
