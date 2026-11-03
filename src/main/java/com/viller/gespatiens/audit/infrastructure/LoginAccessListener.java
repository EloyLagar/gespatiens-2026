package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.access.users.application.AccessEvents.LoginAttempted;
import com.viller.gespatiens.access.users.application.AccessEvents.PasswordChanged;
import com.viller.gespatiens.audit.application.RecordAccessUseCase;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Lo que avisa el módulo de acceso (SpringAccessEvents) pasa al registro de accesos: cada intento de entrar y cada
// cambio de contraseña. Si no se puede anotar, la acción sigue su curso: que el registro falle no debe dejar a
// nadie sin entrar. Queda en el log de la aplicación
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
            recordAccess.execute(event.occurredAt(), event.email(), event.userId(), null,
                    AccessOutcome.valueOf(event.outcome().name()), event.origin().ipAddress(),
                    event.origin().userAgent());
        } catch (RuntimeException e) {
            log.error("No se ha podido anotar en el registro de accesos un intento {} de {} desde {}",
                    event.outcome(), event.email(), event.origin().ipAddress(), e);
        }
    }

    @EventListener
    public void on(PasswordChanged event) {
        try {
            recordAccess.execute(event.occurredAt(), event.email(), event.userId(), event.actorId(),
                    outcomeOf(event.kind()), event.origin().ipAddress(), event.origin().userAgent());
        } catch (RuntimeException e) {
            log.error("No se ha podido anotar en el registro de accesos el cambio de contraseña {} de {}",
                    event.kind(), event.email(), e);
        }
    }

    static AccessOutcome outcomeOf(com.viller.gespatiens.access.users.application.AccessEvents.PasswordChange kind) {
        return switch (kind) {
            case CHANGED -> AccessOutcome.PASSWORD_CHANGED;
            case CHANGE_REJECTED -> AccessOutcome.PASSWORD_CHANGE_REJECTED;
            case SET_FROM_LINK -> AccessOutcome.PASSWORD_SET;
            case RESET_BY_ADMIN -> AccessOutcome.PASSWORD_RESET;
        };
    }
}
