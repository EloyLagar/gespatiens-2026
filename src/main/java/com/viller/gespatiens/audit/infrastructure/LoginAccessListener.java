package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.access.users.application.LoginEvents.LoginAttempted;
import com.viller.gespatiens.audit.application.RecordAccessUseCase;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Cada intento de entrar que avisa el login (SpringLoginEvents) pasa al registro de accesos. Si no se puede anotar,
// el login sigue su curso: que el registro falle no debe dejar a nadie sin entrar. Queda en el log de la aplicación
@Component
public class LoginAccessListener {

    private static final Logger log = LoggerFactory.getLogger(LoginAccessListener.class);

    private final RecordAccessUseCase recordAccess;

    public LoginAccessListener(RecordAccessUseCase recordAccess) {
        this.recordAccess = recordAccess;
    }

    @EventListener
    public void on(LoginAttempted event) {
        try {
            recordAccess.execute(event.occurredAt(), event.email(), event.userId(),
                    AccessOutcome.valueOf(event.outcome().name()), event.origin().ipAddress(),
                    event.origin().userAgent());
        } catch (RuntimeException e) {
            log.error("No se ha podido anotar en el registro de accesos un intento {} de {} desde {}",
                    event.outcome(), event.email(), event.origin().ipAddress(), e);
        }
    }
}
