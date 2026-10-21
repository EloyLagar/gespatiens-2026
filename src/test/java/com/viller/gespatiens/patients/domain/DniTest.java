package com.viller.gespatiens.patients.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DniTest {

    @Test
    void acceptsValidDniAndNormalizesIt() {
        assertEquals("12345678Z", new Dni(" 12345678z ").value());
    }

    @Test
    void acceptsValidNie() {
        assertEquals("X1234567L", new Dni("X1234567L").value());
    }

    @Test
    void rejectsWrongControlLetter() {
        assertThrows(IllegalArgumentException.class, () -> new Dni("12345678A"));
    }

    @Test
    void rejectsWrongFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Dni("1234Z"));
        assertThrows(IllegalArgumentException.class, () -> new Dni(""));
        assertThrows(IllegalArgumentException.class, () -> new Dni(null));
    }
}
