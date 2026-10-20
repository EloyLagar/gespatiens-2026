package com.viller.gespatiens.interventions.infrastructure.config;

import com.viller.gespatiens.interventions.application.AssignTutorsUseCase;
import com.viller.gespatiens.interventions.application.DeleteInterventionUseCase;
import com.viller.gespatiens.interventions.application.EditInterventionUseCase;
import com.viller.gespatiens.interventions.application.GetFollowUpUseCase;
import com.viller.gespatiens.interventions.application.ListFollowUpsUseCase;
import com.viller.gespatiens.interventions.application.RecordInterventionUseCase;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.ResidentDirectory;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí. El Clock lo define NoticeBeanConfiguration
@Configuration
public class InterventionBeanConfiguration {

    @Bean
    public ListFollowUpsUseCase listFollowUpsUseCase(ResidentDirectory interventionsResidentDirectory,
                                                     ResidentTutorsRepository tutorsRepository,
                                                     InterventionRepository interventionRepository,
                                                     StaffDirectory interventionsStaffDirectory) {
        return new ListFollowUpsUseCase(interventionsResidentDirectory, tutorsRepository, interventionRepository,
                interventionsStaffDirectory);
    }

    @Bean
    public GetFollowUpUseCase getFollowUpUseCase(ResidentDirectory interventionsResidentDirectory,
                                                 ResidentTutorsRepository tutorsRepository,
                                                 InterventionRepository interventionRepository,
                                                 StaffDirectory interventionsStaffDirectory) {
        return new GetFollowUpUseCase(interventionsResidentDirectory, tutorsRepository, interventionRepository,
                interventionsStaffDirectory);
    }

    @Bean
    public AssignTutorsUseCase assignTutorsUseCase(ResidentDirectory interventionsResidentDirectory,
                                                   ResidentTutorsRepository tutorsRepository,
                                                   StaffDirectory interventionsStaffDirectory) {
        return new AssignTutorsUseCase(interventionsResidentDirectory, tutorsRepository, interventionsStaffDirectory);
    }

    @Bean
    public RecordInterventionUseCase recordInterventionUseCase(ResidentDirectory interventionsResidentDirectory,
                                                               ResidentTutorsRepository tutorsRepository,
                                                               InterventionRepository interventionRepository,
                                                               StaffDirectory interventionsStaffDirectory,
                                                               Clock clock) {
        return new RecordInterventionUseCase(interventionsResidentDirectory, tutorsRepository, interventionRepository,
                interventionsStaffDirectory, clock);
    }

    @Bean
    public EditInterventionUseCase editInterventionUseCase(InterventionRepository interventionRepository,
                                                           StaffDirectory interventionsStaffDirectory, Clock clock) {
        return new EditInterventionUseCase(interventionRepository, interventionsStaffDirectory, clock);
    }

    @Bean
    public DeleteInterventionUseCase deleteInterventionUseCase(InterventionRepository interventionRepository,
                                                               StaffDirectory interventionsStaffDirectory) {
        return new DeleteInterventionUseCase(interventionRepository, interventionsStaffDirectory);
    }
}
