package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.NotATutorException;
import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.ResidentDirectory;
import com.viller.gespatiens.interventions.domain.ResidentNotFoundException;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Registrar una intervención: solo un tutor del residente y solo mientras está en el centro
public class RecordInterventionUseCase {

    private final ResidentDirectory residentDirectory;
    private final ResidentTutorsRepository tutorsRepository;
    private final InterventionRepository interventionRepository;
    private final StaffDirectory staffDirectory;
    private final Clock clock;

    public RecordInterventionUseCase(ResidentDirectory residentDirectory, ResidentTutorsRepository tutorsRepository,
                                     InterventionRepository interventionRepository, StaffDirectory staffDirectory,
                                     Clock clock) {
        this.residentDirectory = residentDirectory;
        this.tutorsRepository = tutorsRepository;
        this.interventionRepository = interventionRepository;
        this.staffDirectory = staffDirectory;
        this.clock = clock;
    }

    public InterventionView execute(UUID patientId, UUID authorId, LocalDate date, String text) {
        Resident resident = residentDirectory.find(patientId)
                .orElseThrow(() -> new ResidentNotFoundException(patientId));
        if (!tutorsRepository.findByPatient(patientId).isTutor(authorId)) {
            throw new NotATutorException();
        }
        if (!resident.active()) {
            throw new IllegalArgumentException("Solo se registran intervenciones de residentes que están en el centro");
        }
        Intervention saved = interventionRepository.save(
                Intervention.record(patientId, authorId, date, text, clock.instant(), LocalDate.now(clock)));
        return FollowUpViews.of(saved, FollowUpViews.staffOf(staffDirectory, List.of(authorId)), authorId,
                FollowUpViews.isAdministrator(staffDirectory, authorId));
    }
}
