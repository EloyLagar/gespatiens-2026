package com.viller.gespatiens.patients.infrastructure;

import com.viller.gespatiens.patients.domain.StayEvents;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

// Los avisos de estancia como eventos de Spring: quien quiera enterarse (los informes) los escucha con
// @EventListener sin que el módulo de residentes dependa de él
@Component
public class SpringStayEvents implements StayEvents {

    private final ApplicationEventPublisher publisher;

    public SpringStayEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void stayStarted(UUID patientId, LocalDate entryDate) {
        publisher.publishEvent(new StayStarted(patientId, entryDate));
    }

    @Override
    public void stayEnded(UUID patientId, LocalDate entryDate, LocalDate exitDate) {
        publisher.publishEvent(new StayEnded(patientId, entryDate, exitDate));
    }
}
