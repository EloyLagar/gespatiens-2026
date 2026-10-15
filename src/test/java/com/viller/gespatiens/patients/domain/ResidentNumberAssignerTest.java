package com.viller.gespatiens.patients.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResidentNumberAssignerTest {

    private final ResidentNumberAssigner assigner = new ResidentNumberAssigner();

    @Test
    void assignsOneWhenCenterIsEmpty() {
        assertEquals(1, assigner.assignLowestFree(Set.of()));
    }

    @Test
    void assignsLowestGapLeftByADischarge() {
        assertEquals(2, assigner.assignLowestFree(Set.of(1, 3, 4)));
    }

    @Test
    void assignsLastNumberWhenOnlyItIsFree() {
        Set<Integer> allButLast = IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toSet());
        assertEquals(32, assigner.assignLowestFree(allButLast));
    }

    @Test
    void rejectsAdmissionWhenCenterIsFull() {
        Set<Integer> full = IntStream.rangeClosed(1, 32).boxed().collect(Collectors.toSet());
        assertThrows(CenterFullException.class, () -> assigner.assignLowestFree(full));
    }
}
