package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.patients.domain.StayEvents.StayEnded;
import com.viller.gespatiens.patients.domain.StayEvents.StayStarted;
import com.viller.gespatiens.reports.application.StayReportsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Al empezar o acabar una estancia, sus informes se crean o reciben la fecha de salida. Si algo falla aquí,
// el alta o la baja ya están hechas y no se deshacen: las pantallas de informes lo completan al abrirse
@Component
public class StayReportsListener {

    private static final Logger log = LoggerFactory.getLogger(StayReportsListener.class);

    private final StayReportsUseCase stayReports;

    public StayReportsListener(StayReportsUseCase stayReports) {
        this.stayReports = stayReports;
    }

    @EventListener
    public void on(StayStarted event) {
        update(event.patientId());
    }

    @EventListener
    public void on(StayEnded event) {
        update(event.patientId());
    }

    private void update(UUID patientId) {
        try {
            stayReports.stayChanged(patientId);
        } catch (RuntimeException e) {
            log.warn("No se han podido preparar los informes del residente {}", patientId, e);
        }
    }
}
