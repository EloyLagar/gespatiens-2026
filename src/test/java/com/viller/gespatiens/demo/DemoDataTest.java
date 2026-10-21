package com.viller.gespatiens.demo;

import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.dashboard.application.DashboardQueries;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contra PostgreSQL de verdad y en un esquema solo para ella: la demostración se carga en una base de datos vacía
// pasando por todos los casos de uso, así que este test comprueba que sigue cumpliendo sus reglas
@SpringBootTest
@ActiveProfiles("demo")
class DemoDataTest {

    private static final String SCHEMA = "gespatiens_demo_test";

    @DynamicPropertySource
    static void ownSchema(DynamicPropertyRegistry registry) {
        String url = System.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/gespatiens-refactor");
        String base = url.replaceAll("[?&]currentSchema=[^&]*", "");
        registry.add("spring.datasource.url", () -> base + (base.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
    }

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private DashboardQueries dashboardQueries;

    @Test
    void theCentreIsReadyToShow() {
        // Diez en el centro (los antiguos residentes no ocupan plaza)
        assertEquals(10, patientRepository.findOccupiedNumbers().size());
        assertTrue(userRepository.existsByEmail("marta.serra@vientos.es"));
        // El panel tiene qué enseñar: informes de media estancia y dos medidas judiciales que terminan pronto
        assertFalse(dashboardQueries.openMidStayReports().isEmpty());
        assertEquals(2, dashboardQueries.courtMeasures().size());
        assertTrue(reportRepository.findAll().stream().anyMatch(r -> r.getState() == ReportState.CLOSED));
    }
}
