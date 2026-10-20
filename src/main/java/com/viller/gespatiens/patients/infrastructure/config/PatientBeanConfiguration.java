package com.viller.gespatiens.patients.infrastructure.config;

import com.viller.gespatiens.patients.application.AdmitPatientUseCase;
import com.viller.gespatiens.patients.application.DischargePatientUseCase;
import com.viller.gespatiens.patients.application.GetPatientUseCase;
import com.viller.gespatiens.patients.application.ListPatientsUseCase;
import com.viller.gespatiens.patients.application.ReadmitPatientUseCase;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.ResidentNumberAssigner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Los casos de uso y servicios de dominio no llevan anotaciones de Spring: se registran aquí
@Configuration
public class PatientBeanConfiguration {

    @Bean
    public ResidentNumberAssigner residentNumberAssigner() {
        return new ResidentNumberAssigner();
    }

    @Bean
    public AdmitPatientUseCase admitPatientUseCase(PatientRepository patientRepository,
                                                   ResidentNumberAssigner residentNumberAssigner) {
        return new AdmitPatientUseCase(patientRepository, residentNumberAssigner);
    }

    @Bean
    public DischargePatientUseCase dischargePatientUseCase(PatientRepository patientRepository) {
        return new DischargePatientUseCase(patientRepository);
    }

    @Bean
    public ReadmitPatientUseCase readmitPatientUseCase(PatientRepository patientRepository,
                                                       ResidentNumberAssigner residentNumberAssigner) {
        return new ReadmitPatientUseCase(patientRepository, residentNumberAssigner);
    }

    @Bean
    public GetPatientUseCase getPatientUseCase(PatientRepository patientRepository) {
        return new GetPatientUseCase(patientRepository);
    }

    @Bean
    public ListPatientsUseCase listPatientsUseCase(PatientRepository patientRepository) {
        return new ListPatientsUseCase(patientRepository);
    }
}
