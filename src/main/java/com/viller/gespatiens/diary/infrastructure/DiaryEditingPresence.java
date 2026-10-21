package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.EditingPresence;
import com.viller.gespatiens.editing.application.EditLockGuard;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Adaptador entre módulos: los bloqueos de edición (RF8) vistos desde el diario
@Component
public class DiaryEditingPresence implements EditingPresence {

    private final EditLockGuard editLockGuard;

    public DiaryEditingPresence(EditLockGuard editLockGuard) {
        this.editLockGuard = editLockGuard;
    }

    @Override
    public void ensureNotEditedByOther(String resource, UUID employeeId) {
        editLockGuard.ensureNotEditedByOther(resource, employeeId);
    }
}
