package com.viller.gespatiens.interventions.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.InterventionStats;
import com.viller.gespatiens.interventions.domain.ResidentTutors;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contra PostgreSQL de verdad: el borrado e inserción de tutores y la consulta agrupada de las
// estadísticas no se pueden probar en memoria. @Transactional deshace todo al acabar cada test
@SpringBootTest
@Transactional
class InterventionPersistenceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResidentTutorsRepository tutorsRepository;
    @Autowired
    private InterventionRepository interventionRepository;

    // Un antiguo residente (sin número de plaza) para no chocar con las plazas ocupadas de la BD
    private UUID patient() {
        PatientDetails details = new PatientDetails("Prueba", "Persistencia", new Dni("99999999R"),
                LocalDate.of(1990, 1, 1), "C/ Test 1", "699999000", null, null, null, null, null);
        return patientRepository.save(Patient.reconstitute(UUID.randomUUID(), details, null, DAY.minusYears(1),
                DAY.minusDays(1))).getId();
    }

    private UUID employee(String name, String phone) {
        return userRepository.save(User.create(name, name.toLowerCase() + ".persistencia@vientos.es", phone,
                Speciality.SOCIAL_EDUCATOR, "hash")).getId();
    }

    @Test
    void tutorsAreReplacedAndReadBack() {
        UUID patient = patient();
        UUID ana = employee("Ana", "699999001");
        UUID pablo = employee("Pablo", "699999002");

        tutorsRepository.save(ResidentTutors.reconstitute(patient, Set.of(ana, pablo)));
        tutorsRepository.save(ResidentTutors.reconstitute(patient, Set.of(pablo)));

        assertEquals(Set.of(pablo), tutorsRepository.findByPatient(patient).getTutorIds());
        assertEquals(Set.of(pablo), tutorsRepository.findAll().get(patient).getTutorIds());
        assertTrue(tutorsRepository.findByPatient(UUID.randomUUID()).getTutorIds().isEmpty());
    }

    @Test
    void interventionsAreListedNewestFirstWithTheirStats() {
        UUID patient = patient();
        UUID pablo = employee("Pablo", "699999002");
        interventionRepository.save(Intervention.record(patient, pablo, DAY.minusDays(3), "Primera", NOW, DAY));
        interventionRepository.save(Intervention.record(patient, pablo, DAY, "Segunda", NOW, DAY));

        List<Intervention> found = interventionRepository.findByPatient(patient);

        assertEquals(List.of("Segunda", "Primera"), found.stream().map(Intervention::getText).toList());
        assertEquals(new InterventionStats(2, DAY), interventionRepository.statsByPatient().get(patient));
    }
}
