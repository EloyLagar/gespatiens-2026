package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.Resident;

import java.util.List;

// Los informes de un residente, de la estancia más antigua a la más reciente
public record ResidentReports(Resident resident, List<Report> reports) {
}
