package com.viller.gespatiens.access.users.domain;

// Servicio de dominio: que siempre quede al menos un administrador no depende de un empleado
// concreto sino de todos, así que no puede vivir dentro del agregado User
public class AdministratorGuard {

    private final UserRepository userRepository;

    public AdministratorGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Antes de borrar al empleado o quitarle la especialidad de administrador
    public void ensureNotLastAdministrator(User user) {
        if (user.isAdministrator() && userRepository.countBySpeciality(Speciality.ADMINISTRATOR) <= 1) {
            throw new LastAdministratorException();
        }
    }
}
