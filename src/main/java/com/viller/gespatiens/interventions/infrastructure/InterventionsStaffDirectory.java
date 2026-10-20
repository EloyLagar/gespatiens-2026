package com.viller.gespatiens.interventions.infrastructure;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;
import com.viller.gespatiens.interventions.domain.StaffMember;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: los empleados del módulo access/users vistos desde las intervenciones.
// No se llama UsersStaffDirectory para no chocar con el bean del mismo nombre del diario
@Component
public class InterventionsStaffDirectory implements StaffDirectory {

    private final UserRepository userRepository;

    public InterventionsStaffDirectory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<StaffMember> find(UUID employeeId) {
        return userRepository.findById(employeeId).map(InterventionsStaffDirectory::toStaffMember);
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
