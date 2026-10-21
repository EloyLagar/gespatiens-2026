package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportArea;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.StaffMember;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Un informe tal como lo ve un empleado: con su residente, los empleados que aparecen en él (autores,
// quien lo empezó y quien lo cerró; los que ya no están en la plantilla no aparecen), las áreas que puede
// redactar quien mira y si puede cerrarlo, reabrirlo y eliminarlo
public record ReportView(Report report, Resident resident, Map<UUID, StaffMember> staff,
                         Set<ReportArea> writableAreas, boolean manageable) {
}
