package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;
import com.viller.gespatiens.diary.domain.StaffMember;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: los empleados del módulo access/users vistos desde el diario
@Component
public class UsersStaffDirectory implements StaffDirectory {

    private final UserRepository userRepository;

    public UsersStaffDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<StaffMember> find(UUID employeeId) {
        return userRepository.findById(employeeId)
                .map(user -> new StaffMember(user.getId(), user.getName(), user.getSpeciality().name()));
    }
}
