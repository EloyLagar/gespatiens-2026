package com.viller.gespatiens.reports.domain;

// Cada área la redactan los profesionales de su especialidad
public class AreaNotWritableException extends RuntimeException {

    public AreaNotWritableException(ReportArea area, ReportType type) {
        super("Tu especialidad no redacta el apartado \"" + area.title(type) + "\" de los informes");
    }
}
