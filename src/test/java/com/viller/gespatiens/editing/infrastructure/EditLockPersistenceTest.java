package com.viller.gespatiens.editing.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.editing.domain.EditLock;
import com.viller.gespatiens.editing.domain.EditLockRepository;
import com.viller.gespatiens.editing.domain.LockRaceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contra PostgreSQL de verdad: que dos peticiones simultáneas no se queden con el mismo bloqueo lo garantizan
// la clave primaria y la revisión. Sin @Transactional: los choques necesitan transacciones reales
@SpringBootTest
class EditLockPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    @Autowired
    private EditLockRepository repository;
    @Autowired
    private UserRepository userRepository;

    private UUID employee(String name, String phone) {
        return userRepository.save(User.create(name, name.toLowerCase() + ".bloqueos@vientos.es", phone,
                Speciality.PSYCHOLOGIST, "hash")).getId();
    }

    @Test
    void onlyOneOfTwoSimultaneousAcquisitionsWins() {
        UUID marcos = employee("Marcos", "699999201");
        UUID elena = employee("Elena", "699999202");
        String resource = "test:" + UUID.randomUUID();
        try {
            repository.save(EditLock.acquire(resource, marcos, NOW));
            assertThrows(LockRaceException.class, () -> repository.save(EditLock.acquire(resource, elena, NOW)));

            // Dos que leen el mismo bloqueo caducado: solo uno se lo queda
            EditLock first = repository.find(resource).orElseThrow();
            EditLock second = repository.find(resource).orElseThrow();
            Instant later = NOW.plus(EditLock.LEASE).plusSeconds(1);
            first.claim(elena, later);
            repository.save(first);
            second.claim(marcos, later);
            assertThrows(LockRaceException.class, () -> repository.save(second));

            EditLock current = repository.find(resource).orElseThrow();
            assertEquals(elena, current.getHolderId());
            // Soltar el de otra persona no lo borra
            repository.delete(EditLock.reconstitute(resource, marcos, NOW, later, null));
            assertTrue(repository.find(resource).isPresent());
            repository.delete(current);
            assertTrue(repository.find(resource).isEmpty());
        } finally {
            // Borrar a los empleados se lleva sus bloqueos (ON DELETE CASCADE)
            userRepository.findById(marcos).ifPresent(userRepository::delete);
            userRepository.findById(elena).ifPresent(userRepository::delete);
        }
    }
}
