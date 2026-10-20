package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionNotFoundException;
import com.viller.gespatiens.interventions.domain.InterventionNotManageableException;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;

import java.util.UUID;

// Eliminar una intervención: su autor o la administración
public class DeleteInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final StaffDirectory staffDirectory;

    public DeleteInterventionUseCase(InterventionRepository interventionRepository, StaffDirectory staffDirectory) {
        this.interventionRepository = interventionRepository;
        this.staffDirectory = staffDirectory;
    }

    public void execute(UUID interventionId, UUID requesterId) {
        Intervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new InterventionNotFoundException(interventionId));
        if (!intervention.canBeManagedBy(requesterId, FollowUpViews.isAdministrator(staffDirectory, requesterId))) {
            throw new InterventionNotManageableException();
        }
        interventionRepository.delete(intervention);
    }
}
