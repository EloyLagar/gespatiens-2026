package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.application.AccessEvents;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// Los intentos de entrar y los cambios de contraseña como eventos de Spring: el registro de accesos (módulo audit)
// los escucha con @EventListener
@Component
public class SpringAccessEvents implements AccessEvents {

    private final ApplicationEventPublisher publisher;

    public SpringAccessEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void loginAttempted(LoginAttempted attempt) {
        publisher.publishEvent(attempt);
    }

    @Override
    public void passwordChanged(PasswordChanged change) {
        publisher.publishEvent(change);
    }
}
