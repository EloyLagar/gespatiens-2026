package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.StaffMember;

import java.util.List;
import java.util.Optional;

// Lo que hace falta para imprimir un informe: el informe, su residente (la cabecera sale de la ficha)
// y los profesionales que lo han elaborado, por orden de aparición, con su firma escaneada si la tienen
public record PrintableReport(Report report, Resident resident, List<Signer> authors) {

    // signature: el PNG de la firma; vacío si todavía no la ha subido
    public record Signer(StaffMember member, Optional<byte[]> signature) {
    }
}
