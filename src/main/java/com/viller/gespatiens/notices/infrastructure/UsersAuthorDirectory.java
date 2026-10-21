package com.viller.gespatiens.notices.infrastructure;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.notices.domain.Author;
import com.viller.gespatiens.notices.domain.AuthorDirectory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: traduce los empleados del módulo access/users a autores de avisos.
// Es el único punto del módulo de avisos que conoce al de empleados
@Component
public class UsersAuthorDirectory implements AuthorDirectory {

    private final UserRepository userRepository;

    public UsersAuthorDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<Author> find(UUID employeeId) {
        return userRepository.findById(employeeId).map(UsersAuthorDirectory::toAuthor);
    }

    @Override
    public Map<UUID, Author> findAll(Collection<UUID> employeeIds) {
        // La plantilla es pequeña (decenas de personas): una consulta por autor distinto es suficiente
        Map<UUID, Author> authors = new HashMap<>();
        for (UUID id : employeeIds) {
            find(id).ifPresent(author -> authors.put(id, author));
        }
        return authors;
    }

    private static Author toAuthor(User user) {
        return new Author(user.getId(), user.getName(), user.getSpeciality().name(), user.isAdministrator());
    }
}
