package com.viller.gespatiens.editing.application;

import com.viller.gespatiens.editing.domain.EditLock;
import com.viller.gespatiens.editing.domain.EditLockRepository;
import com.viller.gespatiens.editing.domain.EmployeeNames;
import com.viller.gespatiens.editing.domain.LockRaceException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

// Empezar a editar algo, o seguir editándolo (la pantalla lo renueva cada 30 segundos). Si lo está editando
// otra persona, se dice quién
public class AcquireEditLockUseCase {

    private final EditLockRepository repository;
    private final EmployeeNames employeeNames;
    private final Clock clock;

    public AcquireEditLockUseCase(EditLockRepository repository, EmployeeNames employeeNames, Clock clock) {
        this.repository = repository;
        this.employeeNames = employeeNames;
        this.clock = clock;
    }

    public EditLockView execute(String resource, UUID employeeId) {
        EditLock.checkResource(resource);
        Instant now = clock.instant();
        EditLock lock = repository.find(resource).orElse(null);
        if (lock != null && lock.blocks(employeeId, now)) {
            throw EditLockGuard.beingEdited(lock, employeeNames);
        }
        if (lock == null) {
            lock = EditLock.acquire(resource, employeeId, now);
        } else {
            lock.claim(employeeId, now);
        }
        try {
            EditLock saved = repository.save(lock);
            return ListEditLocksUseCase.view(saved, employeeNames, employeeId);
        } catch (LockRaceException race) {
            // Otro empleado se lo ha quedado en el mismo instante: se le nombra como en el caso normal
            EditLock winner = repository.find(resource).orElseThrow(() -> race);
            throw EditLockGuard.beingEdited(winner, employeeNames);
        }
    }
}
