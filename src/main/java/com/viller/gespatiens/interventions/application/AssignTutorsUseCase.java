package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.ResidentDirectory;
import com.viller.gespatiens.interventions.domain.ResidentNotFoundException;
import com.viller.gespatiens.interventions.domain.ResidentTutors;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

// Asignar los tutores de un residente: un educador social y un psicólogo (memoria final). Solo la
// administración (decisión del centro): lo comprueba SecurityConfig
public class AssignTutorsUseCase {

    private final ResidentDirectory residentDirectory;
    private final ResidentTutorsRepository tutorsRepository;
    private final StaffDirectory staffDirectory;

    public AssignTutorsUseCase(ResidentDirectory residentDirectory, ResidentTutorsRepository tutorsRepository,
                               StaffDirectory staffDirectory) {
        this.residentDirectory = residentDirectory;
        this.tutorsRepository = tutorsRepository;
        this.staffDirectory = staffDirectory;
    }

    // null: ese tutor queda sin asignar
    public List<StaffMember> execute(UUID patientId, UUID educatorId, UUID psychologistId) {
        Resident resident = residentDirectory.find(patientId)
                .orElseThrow(() -> new ResidentNotFoundException(patientId));
        if (!resident.active()) {
            throw new IllegalArgumentException("Solo se asignan tutores a residentes que están en el centro");
        }
        Map<UUID, StaffMember> staff = FollowUpViews.staffOf(staffDirectory,
                Stream.of(educatorId, psychologistId).filter(Objects::nonNull).toList());
        ResidentTutors tutors = tutorsRepository.findByPatient(patientId);
        tutors.assign(member(staff, educatorId), member(staff, psychologistId));
        return FollowUpViews.tutors(tutorsRepository.save(tutors), staff);
    }

    private static StaffMember member(Map<UUID, StaffMember> staff, UUID id) {
        if (id == null) {
            return null;
        }
        StaffMember member = staff.get(id);
        if (member == null) {
            throw new IllegalArgumentException("No existe ningún empleado con id " + id);
        }
        return member;
    }
}
