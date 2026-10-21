package com.viller.gespatiens.rota.infrastructure.config;

import com.viller.gespatiens.rota.application.GetRotaUseCase;
import com.viller.gespatiens.rota.application.PlanRotaUseCase;
import com.viller.gespatiens.rota.application.SwapsUseCase;
import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;
import com.viller.gespatiens.rota.domain.RotaRepositories.ShiftSwapRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí. El Clock lo define NoticeBeanConfiguration
@Configuration
public class RotaBeanConfiguration {

    @Bean
    public GetRotaUseCase getRotaUseCase(PlannedShiftRepository shifts, Employee.Directory rotaStaffDirectory) {
        return new GetRotaUseCase(shifts, rotaStaffDirectory);
    }

    @Bean
    public PlanRotaUseCase planRotaUseCase(PlannedShiftRepository shifts, Employee.Directory rotaStaffDirectory) {
        return new PlanRotaUseCase(shifts, rotaStaffDirectory);
    }

    @Bean
    public SwapsUseCase swapsUseCase(PlannedShiftRepository shifts, ShiftSwapRepository swaps,
                                     Employee.Directory rotaStaffDirectory, Clock clock) {
        return new SwapsUseCase(shifts, swaps, rotaStaffDirectory, clock);
    }
}
