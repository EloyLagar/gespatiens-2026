package com.viller.gespatiens.patients.infrastructure.config;

import com.viller.gespatiens.patients.application.AdmitPatientUseCase;
import com.viller.gespatiens.patients.application.DeletePatientUseCase;
import com.viller.gespatiens.patients.application.DischargePatientUseCase;
import com.viller.gespatiens.patients.application.GetPatientUseCase;
import com.viller.gespatiens.patients.application.ListPatientsUseCase;
import com.viller.gespatiens.patients.application.ReadmitPatientUseCase;
import com.viller.gespatiens.patients.application.UpdatePatientDetailsUseCase;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.ResidentNumberAssigner;
import com.viller.gespatiens.patients.domain.StayEvents;
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
                                                   ResidentNumberAssigner residentNumberAssigner,
                                                   StayEvents stayEvents) {
        return new AdmitPatientUseCase(patientRepository, residentNumberAssigner, stayEvents);
    }

    @Bean
    public DischargePatientUseCase dischargePatientUseCase(PatientRepository patientRepository,
                                                           StayEvents stayEvents) {
        return new DischargePatientUseCase(patientRepository, stayEvents);
    }

    @Bean
    public ReadmitPatientUseCase readmitPatientUseCase(PatientRepository patientRepository,
                                                       ResidentNumberAssigner residentNumberAssigner,
                                                       StayEvents stayEvents) {
        return new ReadmitPatientUseCase(patientRepository, residentNumberAssigner, stayEvents);
    }

    @Bean
    public GetPatientUseCase getPatientUseCase(PatientRepository patientRepository) {
        return new GetPatientUseCase(patientRepository);
    }

    @Bean
    public ListPatientsUseCase listPatientsUseCase(PatientRepository patientRepository) {
        return new ListPatientsUseCase(patientRepository);
    }

    @Bean
    public UpdatePatientDetailsUseCase updatePatientDetailsUseCase(PatientRepository patientRepository) {
        return new UpdatePatientDetailsUseCase(patientRepository);
    }

    @Bean
    public DeletePatientUseCase deletePatientUseCase(PatientRepository patientRepository) {
        return new DeletePatientUseCase(patientRepository);
    }
}
