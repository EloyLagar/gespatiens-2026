package com.viller.gespatiens.patients.domain;

import java.time.LocalDate;
import java.util.UUID;

// Puerto de salida: avisa al resto de la aplicación de que empieza o acaba la estancia de un residente
// (los informes de cada estancia se crean y se cierran con ella). Patients no sabe quién escucha
public interface StayEvents {

    void stayStarted(UUID patientId, LocalDate entryDate);

    void stayEnded(UUID patientId, LocalDate entryDate, LocalDate exitDate);

    // Para los tests de los casos de uso que no comprueban los avisos
    static StayEvents none() {
        return new StayEvents() {
            @Override
            public void stayStarted(UUID patientId, LocalDate entryDate) {
            }

            @Override
            public void stayEnded(UUID patientId, LocalDate entryDate, LocalDate exitDate) {
            }
        };
    }

    // Lo que viaja en cada aviso
    record StayStarted(UUID patientId, LocalDate entryDate) {
    }

    record StayEnded(UUID patientId, LocalDate entryDate, LocalDate exitDate) {
    }
}
