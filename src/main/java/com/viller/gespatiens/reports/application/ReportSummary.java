package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.Resident;

// Un informe en un listado: sin el texto de sus apartados, que solo hace falta al abrirlo
public record ReportSummary(Report report, Resident resident) {
}
