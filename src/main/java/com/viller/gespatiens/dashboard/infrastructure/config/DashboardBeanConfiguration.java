package com.viller.gespatiens.dashboard.infrastructure.config;

import com.viller.gespatiens.dashboard.application.DashboardQueries;
import com.viller.gespatiens.dashboard.application.DashboardUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí
@Configuration
public class DashboardBeanConfiguration {

    @Bean
    public DashboardUseCase dashboardUseCase(DashboardQueries dashboardQueries) {
        return new DashboardUseCase(dashboardQueries);
    }
}
