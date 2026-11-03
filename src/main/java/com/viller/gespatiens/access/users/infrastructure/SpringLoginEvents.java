package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.application.LoginEvents;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// Los intentos de entrar como eventos de Spring: el registro de accesos (módulo audit) los escucha con
// @EventListener
@Component
public class SpringLoginEvents implements LoginEvents {

    private final ApplicationEventPublisher publisher;

    public SpringLoginEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void attempted(LoginAttempted attempt) {
        publisher.publishEvent(attempt);
    }
}
