package com.viller.gespatiens.editing.application;

import com.viller.gespatiens.editing.domain.EditLock;
import com.viller.gespatiens.editing.domain.EditLockRepository;
import com.viller.gespatiens.editing.domain.EmployeeNames;
import com.viller.gespatiens.editing.domain.ResourceBeingEditedException;

import java.time.Clock;
import java.util.UUID;

// Lo que usan los demás módulos (a través de sus propios puertos) antes de guardar: si otra persona tiene
// el recurso bloqueado, el guardado se rechaza aunque la pantalla no lo hubiera pedido (otra pestaña, una
// petición hecha a mano...). Quien no tiene el bloqueo pero tampoco lo tiene nadie puede guardar: el control
// de versiones de cada módulo cubre ese caso
public class EditLockGuard {

    private final EditLockRepository repository;
    private final EmployeeNames employeeNames;
    private final Clock clock;

    public EditLockGuard(EditLockRepository repository, EmployeeNames employeeNames, Clock clock) {
        this.repository = repository;
        this.employeeNames = employeeNames;
        this.clock = clock;
    }

    public void ensureNotEditedByOther(String resource, UUID employeeId) {
        repository.find(resource)
                .filter(lock -> lock.blocks(employeeId, clock.instant()))
                .ifPresent(lock -> {
                    throw beingEdited(lock, employeeNames);
                });
    }

    static ResourceBeingEditedException beingEdited(EditLock lock, EmployeeNames names) {
        return new ResourceBeingEditedException(names.nameOf(lock.getHolderId()).orElse(null),
                lock.getAcquiredAt());
    }
}
