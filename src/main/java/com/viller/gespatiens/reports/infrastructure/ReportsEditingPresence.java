package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.editing.application.EditLockGuard;
import com.viller.gespatiens.reports.domain.EditingPresence;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Adaptador entre módulos: los bloqueos de edición (RF8) vistos desde los informes
@Component
public class ReportsEditingPresence implements EditingPresence {

    private final EditLockGuard editLockGuard;

    public ReportsEditingPresence(EditLockGuard editLockGuard) {
        this.editLockGuard = editLockGuard;
    }

    @Override
    public void ensureNotEditedByOther(String resource, UUID employeeId) {
        editLockGuard.ensureNotEditedByOther(resource, employeeId);
    }
}
