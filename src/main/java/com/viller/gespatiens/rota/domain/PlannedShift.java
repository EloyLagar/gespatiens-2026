package com.viller.gespatiens.rota.domain;

import java.time.LocalDate;
import java.util.UUID;

// Un turno del cuadrante: a quién le toca trabajar qué parte de qué día. Es planificación, no un registro de
// lo trabajado (decisión del centro: nada de fichajes)
public class PlannedShift {

    private final UUID id;
    private final LocalDate date;
    private final ShiftPart part;
    private UUID employeeId;

    private PlannedShift(UUID id, LocalDate date, ShiftPart part, UUID employeeId) {
        if (id == null || date == null || part == null || employeeId == null) {
            throw new IllegalArgumentException("El turno necesita día, parte del día y trabajador");
        }
        this.id = id;
        this.date = date;
        this.part = part;
        this.employeeId = employeeId;
    }

    public static PlannedShift plan(LocalDate date, ShiftPart part, UUID employeeId) {
        return new PlannedShift(UUID.randomUUID(), date, part, employeeId);
    }

    public static PlannedShift reconstitute(UUID id, LocalDate date, ShiftPart part, UUID employeeId) {
        return new PlannedShift(id, date, part, employeeId);
    }

    // Un cambio de turno aceptado: el turno pasa a otro trabajador
    void handOverTo(UUID newEmployeeId) {
        if (newEmployeeId == null) {
            throw new IllegalArgumentException("Indica a quién pasa el turno");
        }
        this.employeeId = newEmployeeId;
    }

    public boolean isOf(UUID employee) {
        return employeeId.equals(employee);
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public ShiftPart getPart() {
        return part;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }
}
