package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.application.AdmitPatientUseCase;
import com.viller.gespatiens.patients.domain.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final AdmitPatientUseCase admitPatientUseCase;

    public PatientController(AdmitPatientUseCase admitPatientUseCase) {
        this.admitPatientUseCase = admitPatientUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse admit(@RequestBody AdmitPatientRequest request) {
        Patient patient = admitPatientUseCase.execute(request.toDetails(), request.entryDateOrToday());
        return PatientResponse.from(patient);
    }
}
