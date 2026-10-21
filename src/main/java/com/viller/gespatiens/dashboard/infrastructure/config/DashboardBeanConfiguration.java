package com.viller.gespatiens.dashboard.infrastructure.config;

import com.viller.gespatiens.dashboard.application.DashboardQueries;
import com.viller.gespatiens.dashboard.application.DashboardUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Period;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí
@Configuration
public class DashboardBeanConfiguration {

    @Bean
    public DashboardUseCase dashboardUseCase(DashboardQueries dashboardQueries, Clock clock,
                                             @Value("${gespatiens.reports.mid-stay-after}") Period midStayAfter,
                                             @Value("${gespatiens.dashboard.deadline-notice}") Period notice) {
        return new DashboardUseCase(dashboardQueries, clock, midStayAfter, notice);
    }
}
