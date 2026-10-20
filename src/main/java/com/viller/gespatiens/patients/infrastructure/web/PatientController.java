package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.application.AdmitPatientUseCase;
import com.viller.gespatiens.patients.application.DischargePatientUseCase;
import com.viller.gespatiens.patients.application.GetPatientUseCase;
import com.viller.gespatiens.patients.application.ListPatientsUseCase;
import com.viller.gespatiens.patients.application.ReadmitPatientUseCase;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final AdmitPatientUseCase admitPatientUseCase;
    private final DischargePatientUseCase dischargePatientUseCase;
    private final ReadmitPatientUseCase readmitPatientUseCase;
    private final GetPatientUseCase getPatientUseCase;
    private final ListPatientsUseCase listPatientsUseCase;

    public PatientController(AdmitPatientUseCase admitPatientUseCase,
                             DischargePatientUseCase dischargePatientUseCase,
                             ReadmitPatientUseCase readmitPatientUseCase,
                             GetPatientUseCase getPatientUseCase,
                             ListPatientsUseCase listPatientsUseCase) {
        this.admitPatientUseCase = admitPatientUseCase;
        this.dischargePatientUseCase = dischargePatientUseCase;
        this.readmitPatientUseCase = readmitPatientUseCase;
        this.getPatientUseCase = getPatientUseCase;
        this.listPatientsUseCase = listPatientsUseCase;
    }

    // ?status=ACTIVE (por defecto) para los residentes actuales, ?status=FORMER para los antiguos
    @GetMapping
    public List<PatientResponse> list(@RequestParam(defaultValue = "ACTIVE") PatientStatus status) {
        return listPatientsUseCase.execute(status).stream()
                .map(PatientResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable UUID id) {
        return PatientResponse.from(getPatientUseCase.execute(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse admit(@RequestBody AdmitPatientRequest request) {
        Patient patient = admitPatientUseCase.execute(request.toDetails(), request.entryDateOrToday());
        return PatientResponse.from(patient);
    }

    // RF9: la baja no borra al residente, cambia su estado. Por eso es un POST a una acción y no un DELETE
    @PostMapping("/{id}/discharge")
    public PatientResponse discharge(@PathVariable UUID id,
                                     @RequestBody(required = false) DischargePatientRequest request) {
        Patient patient = dischargePatientUseCase.execute(id, DischargePatientRequest.exitDateOrToday(request));
        return PatientResponse.from(patient);
    }

    // Dar de alta de nuevo a un residente antiguo, con los datos de su ficha
    @PostMapping("/{id}/readmit")
    public PatientResponse readmit(@PathVariable UUID id,
                                   @RequestBody(required = false) ReadmitPatientRequest request) {
        Patient patient = readmitPatientUseCase.execute(id, ReadmitPatientRequest.entryDateOrToday(request));
        return PatientResponse.from(patient);
    }
}
