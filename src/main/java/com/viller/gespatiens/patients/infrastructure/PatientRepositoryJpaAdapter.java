package com.viller.gespatiens.patients.infrastructure;

import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class PatientRepositoryJpaAdapter implements PatientRepository {

    private final PatientJpaRepository patientJpaRepository;

    public PatientRepositoryJpaAdapter(PatientJpaRepository patientJpaRepository) {
        this.patientJpaRepository = patientJpaRepository;
    }

    @Override
    public Patient save(Patient patient) {
        PatientJpaEntity saved = patientJpaRepository.save(toEntity(patient));
        return toDomain(saved);
    }

    @Override
    public void delete(Patient patient) {
        patientJpaRepository.deleteById(patient.getId());
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return patientJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Patient> findByStatus(PatientStatus status) {
        List<PatientJpaEntity> entities = switch (status) {
            case ACTIVE -> patientJpaRepository.findByExitDateIsNullOrderByNumber();
            case FORMER -> patientJpaRepository.findByExitDateIsNotNullOrderByExitDateDesc();
        };
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByDni(Dni dni) {
        return patientJpaRepository.existsByDni(dni.value());
    }

    @Override
    public boolean existsByVisitCode(String visitCode) {
        return patientJpaRepository.existsByVisitCode(visitCode);
    }

    @Override
    public boolean existsByDniAndIdNot(Dni dni, UUID id) {
        return patientJpaRepository.existsByDniAndIdNot(dni.value(), id);
    }

    @Override
    public boolean existsByVisitCodeAndIdNot(String visitCode, UUID id) {
        return patientJpaRepository.existsByVisitCodeAndIdNot(visitCode, id);
    }

    @Override
    public Set<Integer> findOccupiedNumbers() {
        return new HashSet<>(patientJpaRepository.findOccupiedNumbers());
    }

    private PatientJpaEntity toEntity(Patient patient) {
        PatientDetails details = patient.getDetails();
        PatientJpaEntity entity = new PatientJpaEntity();
        entity.setId(patient.getId());
        entity.setNumber(patient.getNumber());
        entity.setName(details.name());
        entity.setSurname(details.surname());
        entity.setDni(details.dni().value());
        entity.setBirthDate(details.birthDate());
        entity.setAddress(details.address());
        entity.setPhoneNumber(details.phoneNumber());
        entity.setSip(details.sip());
        entity.setVisitCode(details.visitCode());
        entity.setBelongings(details.belongings());
        entity.setAbuseSubstances(details.abuseSubstances());
        entity.setExtraInfo(details.extraInfo());
        entity.setEntryDate(patient.getEntryDate());
        entity.setExitDate(patient.getExitDate());
        return entity;
    }

    private Patient toDomain(PatientJpaEntity entity) {
        PatientDetails details = new PatientDetails(
                entity.getName(),
                entity.getSurname(),
                new Dni(entity.getDni()),
                entity.getBirthDate(),
                entity.getAddress(),
                entity.getPhoneNumber(),
                entity.getSip(),
                entity.getVisitCode(),
                entity.getBelongings(),
                entity.getAbuseSubstances(),
                entity.getExtraInfo());
        return Patient.reconstitute(
                entity.getId(),
                details,
                entity.getNumber(),
                entity.getEntryDate(),
                entity.getExitDate());
    }
}
