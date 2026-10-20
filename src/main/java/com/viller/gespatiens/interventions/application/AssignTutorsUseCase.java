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
import java.util.Set;
import java.util.UUID;

// Asignar los tutores de un residente. Quién puede (la administración y los educadores) lo comprueba
// SecurityConfig. Por decisión del centro, solo los educadores sociales son tutores
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

    public List<StaffMember> execute(UUID patientId, Set<UUID> tutorIds) {
        Resident resident = residentDirectory.find(patientId)
                .orElseThrow(() -> new ResidentNotFoundException(patientId));
        if (!resident.active()) {
            throw new IllegalArgumentException("Solo se asignan tutores a residentes que están en el centro");
        }
        Set<UUID> ids = tutorIds == null ? Set.of() : tutorIds;
        Map<UUID, StaffMember> staff = FollowUpViews.staffOf(staffDirectory, ids);
        for (UUID id : ids) {
            StaffMember member = staff.get(id);
            if (member == null) {
                throw new IllegalArgumentException("No existe ningún empleado con id " + id);
            }
            if (!member.isSocialEducator()) {
                throw new IllegalArgumentException("Solo los educadores sociales pueden ser tutores: " + member.name()
                        + " no lo es");
            }
        }
        ResidentTutors tutors = tutorsRepository.findByPatient(patientId);
        tutors.assign(ids);
        return FollowUpViews.tutors(tutorsRepository.save(tutors), staff);
    }
}
