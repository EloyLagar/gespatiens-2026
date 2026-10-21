package com.viller.gespatiens.rota.infrastructure;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.rota.domain.Employee;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: los empleados del módulo access/users vistos desde el cuadrante
@Component
public class RotaStaffDirectory implements Employee.Directory {

    private final UserRepository userRepository;

    public RotaStaffDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<Employee> find(UUID id) {
        return userRepository.findById(id).map(RotaStaffDirectory::toEmployee);
    }

    @Override
    public List<Employee> findAll() {
        return userRepository.findAll().stream().map(RotaStaffDirectory::toEmployee).toList();
    }

    private static Employee toEmployee(User user) {
        return new Employee(user.getId(), user.getName(), user.getSpeciality().name());
    }
}
