package com.viller.gespatiens.notices.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de empleados: quién es cada autor y si es de administración.
// La implementación (UsersAuthorDirectory) vive en infrastructure
public interface AuthorDirectory {
    Optional<Author> find(UUID employeeId);
    // Los autores de una lista de avisos en una sola consulta
    Map<UUID, Author> findAll(Collection<UUID> employeeIds);
}
