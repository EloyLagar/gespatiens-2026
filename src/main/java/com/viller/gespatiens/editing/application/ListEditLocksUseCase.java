package com.viller.gespatiens.editing.application;

import com.viller.gespatiens.editing.domain.EditLock;
import com.viller.gespatiens.editing.domain.EditLockRepository;
import com.viller.gespatiens.editing.domain.EmployeeNames;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// Qué se está editando ahora dentro de algo (p. ej. todos los apartados de un informe), para que la pantalla
// avise "Marta lo está editando" antes de que nadie lo intente
public class ListEditLocksUseCase {

    private final EditLockRepository repository;
    private final EmployeeNames employeeNames;
    private final Clock clock;

    public ListEditLocksUseCase(EditLockRepository repository, EmployeeNames employeeNames, Clock clock) {
        this.repository = repository;
        this.employeeNames = employeeNames;
        this.clock = clock;
    }

    public List<EditLockView> execute(String prefix, UUID viewerId) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Indica qué recursos quieres consultar");
        }
        Instant now = clock.instant();
        // Hay como mucho unas decenas de bloqueos a la vez: se filtran en memoria
        return repository.findAll().stream()
                .filter(lock -> lock.getResource().startsWith(prefix) && lock.isActive(now))
                .sorted(Comparator.comparing(EditLock::getResource))
                .map(lock -> view(lock, employeeNames, viewerId))
                .toList();
    }

    static EditLockView view(EditLock lock, EmployeeNames names, UUID viewerId) {
        return new EditLockView(lock.getResource(), lock.getHolderId(), names.nameOf(lock.getHolderId()).orElse(null),
                lock.getAcquiredAt(), lock.getExpiresAt(), lock.getHolderId().equals(viewerId));
    }
}
