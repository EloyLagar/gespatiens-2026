package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionNotFoundException;
import com.viller.gespatiens.interventions.domain.InterventionNotManageableException;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

// Corregir una intervención: su autor o la administración, como los avisos
public class EditInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final StaffDirectory staffDirectory;
    private final Clock clock;

    public EditInterventionUseCase(InterventionRepository interventionRepository, StaffDirectory staffDirectory,
                                   Clock clock) {
        this.interventionRepository = interventionRepository;
        this.staffDirectory = staffDirectory;
        this.clock = clock;
    }

    public InterventionView execute(UUID interventionId, UUID editorId, LocalDate date, String text) {
        Intervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new InterventionNotFoundException(interventionId));
        boolean editorIsAdmin = FollowUpViews.isAdministrator(staffDirectory, editorId);
        if (!intervention.canBeManagedBy(editorId, editorIsAdmin)) {
            throw new InterventionNotManageableException();
        }
        intervention.edit(date, text, clock.instant(), LocalDate.now(clock));
        Intervention saved = interventionRepository.save(intervention);
        return FollowUpViews.of(saved, FollowUpViews.staffOf(staffDirectory, saved.getAuthorId().stream().toList()),
                editorId, editorIsAdmin);
    }
}
