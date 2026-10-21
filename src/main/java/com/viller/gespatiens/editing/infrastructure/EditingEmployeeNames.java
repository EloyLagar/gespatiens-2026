package com.viller.gespatiens.editing.infrastructure;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.editing.domain.EmployeeNames;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: el nombre de un empleado del módulo access/users
@Component
public class EditingEmployeeNames implements EmployeeNames {

    private final UserRepository userRepository;

    public EditingEmployeeNames(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<String> nameOf(UUID employeeId) {
        return userRepository.findById(employeeId).map(User::getName);
    }
}
