package com.viller.gespatiens.reports.domain;

import java.util.UUID;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(UUID reportId) {
        super("No existe ningún informe con id " + reportId);
    }
}
