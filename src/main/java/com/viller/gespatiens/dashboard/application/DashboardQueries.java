package com.viller.gespatiens.dashboard.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Puerto de lectura del panel de control: cifras que cruzan varios módulos (turnos, avisos, residentes,
// informes, intervenciones). Es solo lectura, así que se consulta directamente la base de datos en lugar de
// cargar agregados enteros para contarlos
public interface DashboardQueries {

    // Turnos de cada empleado entre dos días (ambos incluidos), por parte del día. Solo quien hizo alguno
    List<ShiftCount> shiftsByEmployee(LocalDate from, LocalDate to);

    // Avisos publicados cada semana: lunes de la semana -> número. Las semanas sin avisos no aparecen
    Map<LocalDate, Integer> noticesByWeek(LocalDate fromMonday, LocalDate toSunday);

    int activeResidents();

    // Residentes en el centro a los que les falta alguno de sus dos tutores
    int activeResidentsWithoutBothTutors();

    int reportsInProgress();

    int interventionsBetween(LocalDate from, LocalDate to);

    record ShiftCount(UUID employeeId, String name, String speciality, int morning, int afternoon, int night) {
        public int total() {
            return morning + afternoon + night;
        }
    }
}
