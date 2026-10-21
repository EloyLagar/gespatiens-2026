package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.audit.application.AuditDirectory;
import com.viller.gespatiens.audit.application.RecordAuditEntryUseCase;
import com.viller.gespatiens.audit.domain.AuditAction;
import com.viller.gespatiens.audit.domain.AuditEntry;
import com.viller.gespatiens.audit.domain.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Sin Spring: peticiones simuladas y un registro en memoria
class AuditInterceptorTest {

    private static final UUID ME = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID REPORT = UUID.randomUUID();
    private static final UUID INTERVENTION = UUID.randomUUID();

    private final List<AuditEntry> log = new ArrayList<>();
    private final AuditLog auditLog = new AuditLog() {
        @Override
        public void append(AuditEntry entry) {
            log.add(entry);
        }

        @Override
        public Page search(Filter filter, int page, int size) {
            throw new UnsupportedOperationException();
        }
    };
    // La intervención "existe" hasta que se borra: después ya no se sabría de quién era
    private boolean interventionExists = true;
    private final AuditDirectory directory = new AuditDirectory() {
        @Override
        public Map<UUID, String> employeeNames(Collection<UUID> ids) {
            return Map.of();
        }

        @Override
        public Map<UUID, Resident> residents(Collection<UUID> ids) {
            return Map.of();
        }

        @Override
        public Optional<UUID> residentOfReport(UUID reportId) {
            return reportId.equals(REPORT) ? Optional.of(PATIENT) : Optional.empty();
        }

        @Override
        public Optional<UUID> residentOfIntervention(UUID interventionId) {
            return interventionExists && interventionId.equals(INTERVENTION) ? Optional.of(PATIENT) : Optional.empty();
        }
    };
    private final AuditInterceptor interceptor = new AuditInterceptor(
            new RecordAuditEntryUseCase(auditLog, Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC)),
            directory);

    private void perform(String method, String path, int status, Runnable handler) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setUserPrincipal(() -> ME.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());
        handler.run();
        response.setStatus(status);
        if (request.getAttribute("created") != null) {
            request.setAttribute(AuditInterceptor.CREATED_ID, request.getAttribute("created"));
        }
        interceptor.afterCompletion(request, response, new Object(), null);
    }

    @Test
    void openingARecordIsLoggedWithWhoAndWhen() {
        perform("GET", "/api/patients/" + PATIENT, 200, () -> { });

        assertEquals(1, log.size());
        AuditEntry entry = log.getFirst();
        assertEquals(ME, entry.getActorId());
        assertEquals(AuditAction.VIEW_RECORD, entry.getAction());
        assertEquals(PATIENT, entry.getPatientId());
        assertEquals(Instant.parse("2026-10-21T10:00:00Z"), entry.getOccurredAt());
    }

    @Test
    void aReportIsLoggedUnderItsResident() {
        perform("GET", "/api/reports/" + REPORT + "/pdf", 200, () -> { });

        assertEquals(AuditAction.DOWNLOAD_REPORT, log.getFirst().getAction());
        assertEquals(PATIENT, log.getFirst().getPatientId());
        assertEquals(REPORT, log.getFirst().getResourceId());
    }

    // El residente se averigua antes de borrar: después ya no existiría la intervención
    @Test
    void deletingAnInterventionStillKnowsWhoseItWas() {
        perform("DELETE", "/api/interventions/" + INTERVENTION, 204, () -> interventionExists = false);

        assertEquals(AuditAction.EDIT_FOLLOW_UP, log.getFirst().getAction());
        assertEquals(PATIENT, log.getFirst().getPatientId());
    }

    @Test
    void failedRequestsAndListsAreNotLogged() {
        perform("GET", "/api/patients/" + PATIENT, 404, () -> { });
        perform("PUT", "/api/patients/" + PATIENT, 400, () -> { });
        perform("GET", "/api/patients", 200, () -> { });
        perform("GET", "/api/notices", 200, () -> { });
        perform("GET", "/api/patients/no-es-un-id", 200, () -> { });

        assertEquals(1, log.size());
        assertNull(log.getFirst().getPatientId());
    }

    @Test
    void matchesTheRightRule() {
        assertEquals(AuditAction.ADMIT, AuditRules.match("POST", "/api/patients").orElseThrow().rule().action());
        assertEquals(AuditAction.VIEW_REFERRAL,
                AuditRules.match("GET", "/api/patients/" + PATIENT + "/referral").orElseThrow().rule().action());
        assertEquals(AuditAction.DOWNLOAD_REPORT,
                AuditRules.match("GET", "/api/reports/residents/" + PATIENT + "/pdf").orElseThrow().rule().action());
        assertEquals(AuditAction.ASSIGN_TUTORS,
                AuditRules.match("PUT", "/api/interventions/residents/" + PATIENT + "/tutors").orElseThrow().rule().action());
        assertTrue(AuditRules.match("GET", "/api/reports").isEmpty());
        assertTrue(AuditRules.match("GET", "/api/diary/2026-10-21").isEmpty());
    }
}
