package com.viller.gespatiens.rota.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;
import com.viller.gespatiens.rota.domain.RotaRepositories.ShiftSwapRepository;
import com.viller.gespatiens.rota.domain.ShiftPart;
import com.viller.gespatiens.rota.domain.ShiftSwap;
import com.viller.gespatiens.rota.domain.SwapStatus;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contra PostgreSQL de verdad: la migración V16, sus restricciones y las consultas JPQL de los cambios.
// Se usan fechas lejanas para no mezclarse con el cuadrante real. @Transactional lo deshace todo al acabar
@SpringBootTest
@Transactional
class RotaPersistenceTest {

    private static final LocalDate DAY = LocalDate.of(2099, 3, 10);
    private static final Instant NOW = Instant.parse("2099-03-01T10:00:00Z");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlannedShiftRepository shiftRepository;
    @Autowired
    private ShiftSwapRepository swapRepository;

    private UUID employee(String name, String phone) {
        return userRepository.save(User.create(name, name.toLowerCase() + ".cuadrante@vientos.es", phone,
                Speciality.SOCIAL_EDUCATOR, "hash")).getId();
    }

    @Test
    void shiftsAreReadByRangeInOrder() {
        UUID ana = employee("Ana", "699999101");
        shiftRepository.save(PlannedShift.plan(DAY, ShiftPart.NIGHT, ana));
        PlannedShift morning = shiftRepository.save(PlannedShift.plan(DAY, ShiftPart.MORNING, ana));
        shiftRepository.save(PlannedShift.plan(DAY.plusMonths(1), ShiftPart.MORNING, ana));

        List<PlannedShift> found = shiftRepository.findBetween(DAY.withDayOfMonth(1), DAY.withDayOfMonth(31));

        assertEquals(List.of(ShiftPart.MORNING, ShiftPart.NIGHT), found.stream().map(PlannedShift::getPart).toList());
        assertTrue(shiftRepository.exists(DAY, ShiftPart.MORNING, ana));
        shiftRepository.delete(morning);
        assertFalse(shiftRepository.exists(DAY, ShiftPart.MORNING, ana));
    }

    @Test
    void swapsAreFoundByPersonAndByShift() {
        UUID ana = employee("Ana", "699999101");
        UUID pablo = employee("Pablo", "699999102");
        PlannedShift mine = shiftRepository.save(PlannedShift.plan(DAY, ShiftPart.MORNING, pablo));
        PlannedShift theirs = shiftRepository.save(PlannedShift.plan(DAY.plusDays(1), ShiftPart.NIGHT, ana));

        ShiftSwap swap = swapRepository.save(ShiftSwap.propose(mine, pablo, ana, theirs, NOW, NOW.atZone(
                java.time.ZoneOffset.UTC).toLocalDate()));

        assertEquals(1, swapRepository.findInvolving(ana).size());
        assertEquals(1, swapRepository.findPendingForShifts(Set.of(theirs.getId())).size());

        swap.decline(ana, NOW.plusSeconds(60));
        swapRepository.save(swap);

        assertEquals(SwapStatus.DECLINED, swapRepository.findById(swap.getId()).orElseThrow().getStatus());
        assertTrue(swapRepository.findPendingForShifts(Set.of(mine.getId(), theirs.getId())).isEmpty());
        assertTrue(swapRepository.findPendingForShifts(Set.of()).isEmpty());
    }
}
