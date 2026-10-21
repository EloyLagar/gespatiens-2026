package com.viller.gespatiens.patients.domain;

import java.util.Set;

// Servicio de dominio: las reglas RF10 y RF11 no dependen de un residente concreto
// sino de todos los activos, así que no pueden vivir dentro del agregado Patient
public class ResidentNumberAssigner {

    // RF11: el menor número libre entre 1 y 32. RF10: si no queda ninguno, el centro está lleno
    public int assignLowestFree(Set<Integer> occupiedNumbers) {
        for (int number = 1; number <= Patient.MAX_RESIDENTS; number++) {
            if (!occupiedNumbers.contains(number)) {
                return number;
            }
        }
        throw new CenterFullException();
    }
}
