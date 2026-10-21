package com.viller.gespatiens.editing.domain;

import java.util.List;
import java.util.Optional;

public interface EditLockRepository {
    Optional<EditLock> find(String resource);
    // Si otra petición lo ha cogido o renovado a la vez, lanza LockRaceException
    EditLock save(EditLock lock);
    void delete(EditLock lock);
    List<EditLock> findAll();
}
