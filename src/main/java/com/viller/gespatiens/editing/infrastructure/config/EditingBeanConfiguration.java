package com.viller.gespatiens.editing.infrastructure.config;

import com.viller.gespatiens.editing.application.AcquireEditLockUseCase;
import com.viller.gespatiens.editing.application.EditLockGuard;
import com.viller.gespatiens.editing.application.ListEditLocksUseCase;
import com.viller.gespatiens.editing.application.ReleaseEditLockUseCase;
import com.viller.gespatiens.editing.domain.EditLockRepository;
import com.viller.gespatiens.editing.domain.EmployeeNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí. El Clock lo define NoticeBeanConfiguration
@Configuration
public class EditingBeanConfiguration {

    @Bean
    public AcquireEditLockUseCase acquireEditLockUseCase(EditLockRepository repository, EmployeeNames names,
                                                         Clock clock) {
        return new AcquireEditLockUseCase(repository, names, clock);
    }

    @Bean
    public ReleaseEditLockUseCase releaseEditLockUseCase(EditLockRepository repository) {
        return new ReleaseEditLockUseCase(repository);
    }

    @Bean
    public ListEditLocksUseCase listEditLocksUseCase(EditLockRepository repository, EmployeeNames names,
                                                     Clock clock) {
        return new ListEditLocksUseCase(repository, names, clock);
    }

    // Lo usan los informes y el diario antes de guardar
    @Bean
    public EditLockGuard editLockGuard(EditLockRepository repository, EmployeeNames names, Clock clock) {
        return new EditLockGuard(repository, names, clock);
    }
}
