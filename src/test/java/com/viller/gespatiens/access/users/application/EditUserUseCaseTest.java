package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.AdministratorGuard;
import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.LastAdministratorException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class EditUserUseCaseTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final EditUserUseCase useCase = new EditUserUseCase(repository, new AdministratorGuard(repository));

    private User save(String name, String email, String phone, Speciality speciality) {
        return repository.save(User.create(name, email, phone, speciality, "hash"));
    }

    @Test
    void administratorChangesEmailAndSpeciality() {
        User ana = save("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST);

        User edited = useCase.execute(ana.getId(), "Ana", "ana.lopez@vientos.es", "600111222",
                Speciality.SOCIAL_WORKER);

        assertEquals("ana.lopez@vientos.es", edited.getEmail());
        assertEquals(Speciality.SOCIAL_WORKER, edited.getSpeciality());
    }

    @Test
    void rejectsEmailOfAnotherEmployee() {
        save("Luis", "luis@vientos.es", "600333444", Speciality.SOCIAL_EDUCATOR);
        User ana = save("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST);

        assertThrows(EmailAlreadyRegisteredException.class, () ->
                useCase.execute(ana.getId(), "Ana", "luis@vientos.es", "600111222", Speciality.PSYCHOLOGIST));
    }

    @Test
    void cannotDemoteTheLastAdministrator() {
        User admin = save("Admin", "admin@vientos.es", "600000000", Speciality.ADMINISTRATOR);

        assertThrows(LastAdministratorException.class, () ->
                useCase.execute(admin.getId(), "Admin", "admin@vientos.es", "600000000", Speciality.PSYCHOLOGIST));
        assertEquals(Speciality.ADMINISTRATOR, repository.findById(admin.getId()).orElseThrow().getSpeciality());
    }

    @Test
    void canDemoteAnAdministratorIfAnotherRemains() {
        save("Admin", "admin@vientos.es", "600000000", Speciality.ADMINISTRATOR);
        User second = save("Segundo", "segundo@vientos.es", "600000001", Speciality.ADMINISTRATOR);

        User edited = useCase.execute(second.getId(), "Segundo", "segundo@vientos.es", "600000001",
                Speciality.PSYCHOLOGIST);

        assertEquals(Speciality.PSYCHOLOGIST, edited.getSpeciality());
    }

    @Test
    void lastAdministratorCanStillEditOtherFields() {
        User admin = save("Admin", "admin@vientos.es", "600000000", Speciality.ADMINISTRATOR);

        User edited = useCase.execute(admin.getId(), "Administración", "admin@vientos.es", "600000000",
                Speciality.ADMINISTRATOR);

        assertEquals("Administración", edited.getName());
    }
}
