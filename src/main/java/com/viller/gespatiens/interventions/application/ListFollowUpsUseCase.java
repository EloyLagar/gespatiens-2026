package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.InterventionStats;
import com.viller.gespatiens.interventions.domain.ResidentDirectory;
import com.viller.gespatiens.interventions.domain.ResidentTutors;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// El apartado "Intervenciones": los residentes que están en el centro, por número de plaza, con sus tutores
// y cuándo fue su última intervención
public class ListFollowUpsUseCase {

    private final ResidentDirectory residentDirectory;
    private final ResidentTutorsRepository tutorsRepository;
    private final InterventionRepository interventionRepository;
    private final StaffDirectory staffDirectory;

    public ListFollowUpsUseCase(ResidentDirectory residentDirectory, ResidentTutorsRepository tutorsRepository,
                                InterventionRepository interventionRepository, StaffDirectory staffDirectory) {
        this.residentDirectory = residentDirectory;
        this.tutorsRepository = tutorsRepository;
        this.interventionRepository = interventionRepository;
        this.staffDirectory = staffDirectory;
    }

    public List<FollowUpSummary> execute(UUID viewerId) {
        Map<UUID, ResidentTutors> tutors = tutorsRepository.findAll();
        Map<UUID, InterventionStats> stats = interventionRepository.statsByPatient();
        Map<UUID, StaffMember> staff = FollowUpViews.staffOf(staffDirectory,
                tutors.values().stream().flatMap(t -> t.getTutorIds().stream()).toList());
        return residentDirectory.findActive().stream()
                .map(resident -> {
                    ResidentTutors own = tutors.getOrDefault(resident.id(), ResidentTutors.none(resident.id()));
                    Optional<InterventionStats> residentStats = Optional.ofNullable(stats.get(resident.id()));
                    return new FollowUpSummary(
                            resident,
                            FollowUpViews.tutors(own, staff),
                            residentStats.map(InterventionStats::count).orElse(0L),
                            residentStats.map(InterventionStats::last),
                            own.isTutor(viewerId));
                })
                .toList();
    }
}
