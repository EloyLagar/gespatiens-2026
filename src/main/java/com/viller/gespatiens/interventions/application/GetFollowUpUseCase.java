package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.ResidentDirectory;
import com.viller.gespatiens.interventions.domain.ResidentNotFoundException;
import com.viller.gespatiens.interventions.domain.ResidentTutors;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

// El seguimiento de un residente. También de los antiguos: su historial se conserva (RF9)
public class GetFollowUpUseCase {

    private final ResidentDirectory residentDirectory;
    private final ResidentTutorsRepository tutorsRepository;
    private final InterventionRepository interventionRepository;
    private final StaffDirectory staffDirectory;

    public GetFollowUpUseCase(ResidentDirectory residentDirectory, ResidentTutorsRepository tutorsRepository,
                              InterventionRepository interventionRepository, StaffDirectory staffDirectory) {
        this.residentDirectory = residentDirectory;
        this.tutorsRepository = tutorsRepository;
        this.interventionRepository = interventionRepository;
        this.staffDirectory = staffDirectory;
    }

    public FollowUpView execute(UUID patientId, UUID viewerId) {
        Resident resident = residentDirectory.find(patientId)
                .orElseThrow(() -> new ResidentNotFoundException(patientId));
        ResidentTutors tutors = tutorsRepository.findByPatient(patientId);
        List<Intervention> interventions = interventionRepository.findByPatient(patientId);
        Map<UUID, StaffMember> staff = FollowUpViews.staffOf(staffDirectory, Stream.concat(
                tutors.getTutorIds().stream(),
                interventions.stream().flatMap(i -> i.getAuthorId().stream())).toList());
        boolean viewerIsAdmin = FollowUpViews.isAdministrator(staffDirectory, viewerId);
        return new FollowUpView(
                resident,
                FollowUpViews.tutors(tutors, staff),
                interventions.stream().map(i -> FollowUpViews.of(i, staff, viewerId, viewerIsAdmin)).toList(),
                tutors.isTutor(viewerId));
    }
}
