package com.viller.gespatiens.demo;

import com.viller.gespatiens.patients.domain.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// La demo publicada: lo que cambien las visitas desaparece al día siguiente. En su propio esquema porque lo vacía
@SpringBootTest(properties = "gespatiens.demo.daily-reset=true")
@ActiveProfiles("demo")
class DemoDataResetTest {

    private static final String SCHEMA = "gespatiens_demo_reset_test";

    @DynamicPropertySource
    static void ownSchema(DynamicPropertyRegistry registry) {
        String url = System.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/gespatiens-refactor");
        String base = url.replaceAll("[?&]currentSchema=[^&]*", "");
        registry.add("spring.datasource.url", () -> base + (base.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
    }

    @Autowired
    private DemoData demoData;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theNextDayEverythingIsLoadedAgain() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        jdbc.update("DELETE FROM notices");
        jdbc.update("UPDATE demo_loads SET loaded_on = ?", today.minusDays(1));

        demoData.refresh();

        assertEquals(10, patientRepository.findOccupiedNumbers().size());
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM notices", Integer.class) > 0);
        assertEquals(today, jdbc.queryForObject("SELECT MAX(loaded_on) FROM demo_loads", LocalDate.class));
    }

    @Test
    void theSameDayNothingIsTouched() {
        jdbc.update("DELETE FROM notices");

        demoData.refresh();

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notices", Integer.class));
    }
}
