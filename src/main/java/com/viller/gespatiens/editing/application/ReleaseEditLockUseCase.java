package com.viller.gespatiens.editing.application;

import com.viller.gespatiens.editing.domain.EditLockRepository;

import java.util.UUID;

// Terminar de editar (guardar, cancelar o salir de la página). Solo libera el bloqueo propio: soltar el de
// otra persona no hace nada
public class ReleaseEditLockUseCase {

    private final EditLockRepository repository;

    public ReleaseEditLockUseCase(EditLockRepository repository) {
        this.repository = repository;
    }

    public void execute(String resource, UUID employeeId) {
        repository.find(resource)
                .filter(lock -> lock.getHolderId().equals(employeeId))
                .ifPresent(repository::delete);
    }
}
