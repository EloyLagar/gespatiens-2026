package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.ResidentDirectory;

import java.util.UUID;

// "Desde la ficha de cada usuario se debe poder acceder a modificar los informes". También los de los
// antiguos residentes: sus datos se conservan (RF9) y el informe final se termina al salir
public class ListResidentReportsUseCase {

    private final StayReportsUseCase stayReports;
    private final ResidentDirectory residentDirectory;

    public ListResidentReportsUseCase(StayReportsUseCase stayReports, ResidentDirectory residentDirectory) {
        this.stayReports = stayReports;
        this.residentDirectory = residentDirectory;
    }

    public ResidentReports execute(UUID patientId) {
        Resident resident = ReportViews.resident(residentDirectory, patientId);
        return new ResidentReports(resident, stayReports.ensureCurrentStay(resident));
    }
}
