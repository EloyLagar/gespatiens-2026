package com.viller.gespatiens.patients.domain;

import java.time.LocalDate;
import java.util.UUID;

public class Patient {

    // RF10: el centro solo puede tener 32 residentes dados de alta a la vez
    public static final int MAX_RESIDENTS = 32;

    private final UUID id;
    private PatientDetails details;
    // Número de referencia del 1 al 32 (RF11). Solo lo tienen los residentes activos
    private Integer number;
    private LocalDate entryDate;
    // null mientras el residente sigue en el centro
    private LocalDate exitDate;

    private Patient(UUID id, PatientDetails details, Integer number, LocalDate entryDate, LocalDate exitDate) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser null");
        }
        if (details == null) {
            throw new IllegalArgumentException("Los datos del residente no pueden estar vacíos");
        }
        if (entryDate == null) {
            throw new IllegalArgumentException("La fecha de entrada no puede estar vacía");
        }
        validateState(number, entryDate, exitDate);
        this.id = id;
        this.details = details;
        this.number = number;
        this.entryDate = entryDate;
        this.exitDate = exitDate;
    }

    // Dar de alta a un residente nuevo: el número se lo asigna ResidentNumberAssigner
    public static Patient admit(PatientDetails details, int number, LocalDate entryDate) {
        checkNotFuture(entryDate);
        return new Patient(UUID.randomUUID(), details, number, entryDate, null);
    }

    // Reconstruir un residente que ya existía (leído de BD), activo o antiguo
    public static Patient reconstitute(UUID id, PatientDetails details, Integer number,
                                       LocalDate entryDate, LocalDate exitDate) {
        return new Patient(id, details, number, entryDate, exitDate);
    }

    // RF9: el residente pasa a antiguos conservando sus datos y deja libre su número
    public void discharge(LocalDate exitDate) {
        if (!isActive()) {
            throw new PatientAlreadyDischargedException(id);
        }
        if (exitDate == null) {
            throw new IllegalArgumentException("La fecha de salida no puede estar vacía");
        }
        if (exitDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de salida no puede ser futura");
        }
        validateState(null, entryDate, exitDate);
        this.number = null;
        this.exitDate = exitDate;
    }

    // Solo se borra la información de los usuarios antiguos (memoria, apartado 4.1):
    // a un residente activo hay que darlo de baja antes
    public void ensureCanBeDeleted() {
        if (isActive()) {
            throw new PatientStillActiveException(id);
        }
    }

    // Modificar la ficha. Las validaciones de cada campo ya las hace PatientDetails
    public void updateDetails(PatientDetails details) {
        if (details == null) {
            throw new IllegalArgumentException("Los datos del residente no pueden estar vacíos");
        }
        this.details = details;
    }

    // Un residente antiguo vuelve al centro con un número nuevo (no tiene por qué ser el que tenía).
    // La tabla solo guarda una estancia, así que la nueva entrada sustituye a la anterior
    public void readmit(int number, LocalDate entryDate) {
        if (isActive()) {
            throw new PatientAlreadyActiveException(id);
        }
        if (entryDate == null) {
            throw new IllegalArgumentException("La fecha de entrada no puede estar vacía");
        }
        checkNotFuture(entryDate);
        if (entryDate.isBefore(exitDate)) {
            throw new IllegalArgumentException(
                    "La fecha de reingreso no puede ser anterior a la última salida (" + exitDate + ")");
        }
        validateState(number, entryDate, null);
        this.number = number;
        this.entryDate = entryDate;
        this.exitDate = null;
    }

    // Un alta futura ocuparía plaza y número (RF10, RF11) antes de que la persona llegue, y abriría ya sus informes
    private static void checkNotFuture(LocalDate entryDate) {
        if (entryDate != null && entryDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser futura");
        }
    }

    // Invariante: activo <=> tiene número y no tiene fecha de salida
    private void validateState(Integer number, LocalDate entryDate, LocalDate exitDate) {
        if (exitDate == null) {
            if (number == null || number < 1 || number > MAX_RESIDENTS) {
                throw new IllegalArgumentException(
                        "Un residente activo debe tener un número entre 1 y " + MAX_RESIDENTS);
            }
        } else {
            if (number != null) {
                throw new IllegalArgumentException("Un residente dado de baja no puede tener número asignado");
            }
            if (exitDate.isBefore(entryDate)) {
                throw new IllegalArgumentException("La fecha de salida no puede ser anterior a la de entrada");
            }
        }
    }

    public boolean isActive() {
        return exitDate == null;
    }

    public UUID getId() {
        return id;
    }

    public PatientDetails getDetails() {
        return details;
    }

    public Integer getNumber() {
        return number;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }
}
