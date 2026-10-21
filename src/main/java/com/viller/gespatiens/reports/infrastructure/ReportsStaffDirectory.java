package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.StaffMember;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: los empleados del módulo access/users vistos desde los informes.
// No se llama UsersStaffDirectory para no chocar con el bean del mismo nombre del diario
@Component
public class ReportsStaffDirectory implements StaffDirectory {

    private final UserRepository userRepository;

    public ReportsStaffDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<StaffMember> find(UUID employeeId) {
        return userRepository.findById(employeeId).map(ReportsStaffDirectory::toStaffMember);
    }

    @Override
    public Map<UUID, StaffMember> findAll(Collection<UUID> employeeIds) {
        // La plantilla es pequeña (decenas de personas): una consulta por empleado distinto es suficiente
        Map<UUID, StaffMember> staff = new HashMap<>();
        for (UUID id : employeeIds) {
            find(id).ifPresent(member -> staff.put(id, member));
        }
        return staff;
    }

    private static StaffMember toStaffMember(User user) {
        return new StaffMember(user.getId(), user.getName(), user.getSpeciality().name(), user.isAdministrator());
    }
}
