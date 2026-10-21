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

    // Informes en elaboración de las estancias en curso
    int reportsInProgress();

    int interventionsBetween(LocalDate from, LocalDate to);

    // Residentes en el centro cuyo informe de media estancia de la estancia en curso sigue en elaboración
    List<OpenMidStayReport> openMidStayReports();

    // Residentes en el centro con medida judicial y fecha de fin
    List<CourtMeasure> courtMeasures();

    record OpenMidStayReport(Deadline.Resident resident, LocalDate entryDate, UUID reportId) {
    }

    record CourtMeasure(Deadline.Resident resident, LocalDate endDate, String court) {
    }

    record ShiftCount(UUID employeeId, String name, String speciality, int morning, int afternoon, int night) {
        public int total() {
            return morning + afternoon + night;
        }
    }
}
