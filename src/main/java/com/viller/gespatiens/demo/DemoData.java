package com.viller.gespatiens.demo;

import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.application.SignatureUseCase;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.application.RecordAttendanceUseCase;
import com.viller.gespatiens.diary.application.RecordEvaluationsUseCase;
import com.viller.gespatiens.diary.application.RecordOutingUseCase;
import com.viller.gespatiens.diary.application.RecordReductionUseCase;
import com.viller.gespatiens.diary.application.RecordShiftUseCase;
import com.viller.gespatiens.diary.application.ScheduleActivityUseCase;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.interventions.application.AssignTutorsUseCase;
import com.viller.gespatiens.interventions.application.RecordInterventionUseCase;
import com.viller.gespatiens.notices.application.PublishNoticeUseCase;
import com.viller.gespatiens.patients.application.AdmitPatientUseCase;
import com.viller.gespatiens.patients.application.AuthorizedVisitorsUseCase;
import com.viller.gespatiens.patients.application.DischargePatientUseCase;
import com.viller.gespatiens.patients.application.ReferralUseCase;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.reports.application.CloseReportUseCase;
import com.viller.gespatiens.reports.application.EditReportHeaderUseCase;
import com.viller.gespatiens.reports.application.WriteSectionUseCase;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportArea;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.rota.application.PlanRotaUseCase;
import com.viller.gespatiens.rota.application.SwapsUseCase;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;
import com.viller.gespatiens.rota.domain.ShiftPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

// Datos de demostración para enseñar la aplicación (perfil "demo"): un centro coherente con personal,
// residentes, un mes de diario, valoraciones, intervenciones, informes en distintas fases, avisos y el
// cuadrante. Todo pasa por los casos de uso, con las mismas reglas que en la app, y las fechas se cuentan desde
// el día en que arranca, así que el panel siempre tiene vencimientos cercanos. Solo actúa sobre una base de
// datos vacía: nunca mezcla nada con datos que ya existan. Con gespatiens.demo.daily-reset (la demo publicada en
// internet) además la vuelve a cargar desde cero cada día, para que lo que cambien las visitas no dure
@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {

    // La contraseña de todo el personal de la demostración
    public static final String PASSWORD = "demo1234";
    private static final String FIRST_DNI = dni(70000001);
    private static final ZoneId CENTRE = ZoneId.of("Europe/Madrid");
    private static final int DIARY_DAYS = 30;
    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private final RegisterUserUseCase registerUser;
    private final SignatureUseCase signatures;
    private final AdmitPatientUseCase admitPatient;
    private final DischargePatientUseCase dischargePatient;
    private final ReferralUseCase referrals;
    private final AuthorizedVisitorsUseCase visitors;
    private final PatientRepository patientRepository;
    private final AssignTutorsUseCase assignTutors;
    private final RecordInterventionUseCase recordIntervention;
    private final RecordShiftUseCase recordShift;
    private final ScheduleActivityUseCase scheduleActivity;
    private final RecordAttendanceUseCase recordAttendance;
    private final RecordEvaluationsUseCase recordEvaluations;
    private final RecordReductionUseCase recordReduction;
    private final RecordOutingUseCase recordOuting;
    private final PublishNoticeUseCase publishNotice;
    private final PlanRotaUseCase planRota;
    private final SwapsUseCase swaps;
    private final PlannedShiftRepository plannedShifts;
    private final ReportRepository reportRepository;
    private final WriteSectionUseCase writeSection;
    private final EditReportHeaderUseCase editHeader;
    private final CloseReportUseCase closeReport;
    private final Clock clock;
    private final JdbcTemplate jdbc;
    private final boolean dailyReset;

    private Random random;
    private final Map<String, UUID> staff = new LinkedHashMap<>();
    private final List<Resident> residents = new ArrayList<>();

    public DemoData(RegisterUserUseCase registerUser, SignatureUseCase signatures, AdmitPatientUseCase admitPatient,
                    DischargePatientUseCase dischargePatient, ReferralUseCase referrals,
                    AuthorizedVisitorsUseCase visitors, PatientRepository patientRepository,
                    AssignTutorsUseCase assignTutors, RecordInterventionUseCase recordIntervention,
                    RecordShiftUseCase recordShift, ScheduleActivityUseCase scheduleActivity,
                    RecordAttendanceUseCase recordAttendance, RecordEvaluationsUseCase recordEvaluations,
                    RecordReductionUseCase recordReduction, RecordOutingUseCase recordOuting,
                    PublishNoticeUseCase publishNotice, PlanRotaUseCase planRota, SwapsUseCase swaps,
                    PlannedShiftRepository plannedShifts, ReportRepository reportRepository,
                    WriteSectionUseCase writeSection, EditReportHeaderUseCase editHeader,
                    CloseReportUseCase closeReport, Clock clock, JdbcTemplate jdbc,
                    @Value("${gespatiens.demo.daily-reset:false}") boolean dailyReset) {
        this.registerUser = registerUser;
        this.signatures = signatures;
        this.admitPatient = admitPatient;
        this.dischargePatient = dischargePatient;
        this.referrals = referrals;
        this.visitors = visitors;
        this.patientRepository = patientRepository;
        this.assignTutors = assignTutors;
        this.recordIntervention = recordIntervention;
        this.recordShift = recordShift;
        this.scheduleActivity = scheduleActivity;
        this.recordAttendance = recordAttendance;
        this.recordEvaluations = recordEvaluations;
        this.recordReduction = recordReduction;
        this.recordOuting = recordOuting;
        this.publishNotice = publishNotice;
        this.planRota = planRota;
        this.swaps = swaps;
        this.plannedShifts = plannedShifts;
        this.reportRepository = reportRepository;
        this.writeSection = writeSection;
        this.editHeader = editHeader;
        this.closeReport = closeReport;
        this.clock = clock;
        this.jdbc = jdbc;
        this.dailyReset = dailyReset;
    }

    // Un residente de la demostración: cuándo entró, cuándo salió (null si sigue) y sus tutores
    private record Resident(UUID id, String name, LocalDate entry, LocalDate exit, String educator,
                            String psychologist) {
        boolean presentOn(LocalDate day) {
            return !entry.isAfter(day) && (exit == null || day.isBefore(exit));
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        refresh();
    }

    // Cada noche y al arrancar: la API gratuita se duerme sin visitas y puede no estar despierta a esa hora
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Madrid")
    public synchronized void refresh() {
        LocalDate today = LocalDate.now(clock.withZone(CENTRE));
        if (patientRepository.existsByDni(new Dni(FIRST_DNI))) {
            if (!dailyReset || today.equals(loadedOn())) {
                log.info("Datos de demostración: ya están cargados");
                return;
            }
            wipe();
            log.info("Datos de demostración: se borran los de otro día para cargarlos de nuevo");
        } else if (!patientRepository.findOccupiedNumbers().isEmpty()) {
            log.warn("Datos de demostración: la base de datos ya tiene residentes, no se carga nada");
            return;
        }
        load(today);
    }

    private void load(LocalDate today) {
        random = new Random(2026);
        staff.clear();
        residents.clear();
        staff();
        residents(today);
        diary(today);
        notices();
        rota(today);
        reports(today);
        log.info("Datos de demostración cargados: {} empleados y {} residentes. Contraseña de todos: {}",
                staff.size(), residents.size(), PASSWORD);
        jdbc.update("INSERT INTO demo_loads (loaded_on) VALUES (?)", today);
    }

    private LocalDate loadedOn() {
        return jdbc.queryForObject("SELECT MAX(loaded_on) FROM demo_loads", LocalDate.class);
    }

    // Vacía todas las tablas del esquema menos el historial de Flyway, en una sola sentencia
    private void wipe() {
        List<String> tables = jdbc.queryForList("""
                SELECT quote_ident(table_name) FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'""", String.class);
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " CASCADE");
    }

    // ------------------------------------------------------------------ Personal

    private void staff() {
        employee("Marta Serra", "marta.serra@vientos.es", "600100100", Speciality.ADMINISTRATOR);
        employee("Ana Beltrán", "ana.beltran@vientos.es", "600100101", Speciality.SOCIAL_EDUCATOR);
        employee("Pablo Ortega", "pablo.ortega@vientos.es", "600100102", Speciality.SOCIAL_EDUCATOR);
        employee("Iván Soler", "ivan.soler@vientos.es", "600100103", Speciality.SOCIAL_EDUCATOR);
        employee("Rosa Martí", "rosa.marti@vientos.es", "600100104", Speciality.SOCIAL_EDUCATOR);
        employee("Carmen López", "carmen.lopez@vientos.es", "600100105", Speciality.SOCIAL_WORKER);
        employee("Laura Gil", "laura.gil@vientos.es", "600100106", Speciality.MEDICAL_TEAM);
        employee("Jorge Vidal", "jorge.vidal@vientos.es", "600100107", Speciality.PSYCHOLOGIST);
        employee("Elena Ríos", "elena.rios@vientos.es", "600100108", Speciality.PSYCHOLOGIST);
    }

    private void employee(String name, String email, String phone, Speciality speciality) {
        UUID id = registerUser.execute(name, email, phone, speciality, PASSWORD).getId();
        staff.put(name, id);
        signatures.upload(id, signature(name));
    }

    private UUID who(String name) {
        return staff.get(name);
    }

    // ------------------------------------------------------------------ Residentes

    private void residents(LocalDate today) {
        // Los días que lleva cada uno: los de 70-100 días tienen el informe de media estancia cerca o vencido
        resident("Lucía", "Martínez Soler", 'M', today.minusDays(98), null, "Ana Beltrán", "Jorge Vidal", "Alcohol");
        resident("Alejandro", "Ruiz Peña", 'H', today.minusDays(80), null, "Pablo Ortega", "Elena Ríos", "Cocaína, alcohol");
        resident("Marcos", "Vidal Ferrer", 'H', today.minusDays(71), null, "Ana Beltrán", "Elena Ríos", "Cocaína");
        resident("Sara", "Molina Ortiz", 'M', today.minusDays(60), null, "Iván Soler", "Jorge Vidal", "Alcohol, benzodiacepinas");
        resident("Nerea", "Campos Gil", 'M', today.minusDays(52), null, "Rosa Martí", "Jorge Vidal", "Cannabis, alcohol");
        resident("Daniel", "Ferrer Roig", 'H', today.minusDays(41), null, "Pablo Ortega", "Jorge Vidal", "Heroína (metadona)");
        resident("Raúl", "Sanz Moreno", 'H', today.minusDays(33), null, "Iván Soler", "Elena Ríos", "Alcohol");
        resident("Hugo", "Navarro Peña", 'H', today.minusDays(24), null, "Rosa Martí", "Elena Ríos", "Cocaína");
        resident("Paula", "Ibáñez Roca", 'M', today.minusDays(15), null, "Ana Beltrán", "Jorge Vidal", "Alcohol");
        resident("Iker", "Sanchis Vidal", 'H', today.minusDays(6), null, "Pablo Ortega", "Elena Ríos", "Cocaína, cannabis");
        // Antiguos residentes
        resident("Óscar", "Lozano Prieto", 'H', today.minusDays(280), today.minusDays(35), "Iván Soler", "Jorge Vidal", "Cocaína, alcohol");
        resident("Beatriz", "Molina Cano", 'M', today.minusDays(190), today.minusDays(12), "Rosa Martí", "Elena Ríos", "Alcohol");
        resident("David", "Romero Gil", 'H', today.minusDays(150), today.minusDays(70), "Ana Beltrán", "Jorge Vidal", "Cocaína");

        Resident alejandro = residents.get(1);
        referrals.update(alejandro.id(), "UCA de Torrent", "Inés Navarro", "961000201", true, "Juzgado de lo Penal n.º 3 de Valencia",
                today.plusDays(12), "Medida alternativa a prisión: tratamiento en comunidad terapéutica.");
        Resident daniel = residents.get(5);
        referrals.update(daniel.id(), "UCA de Sagunto", "Tomás Albert", "961000202", true, "Juzgado de lo Penal n.º 1 de Sagunto",
                today.plusDays(26), null);
        for (Resident r : residents.subList(0, 10)) {
            if (r != alejandro && r != daniel) {
                referrals.update(r.id(), "UCA de Valencia", "Sonia Pastor", "961000200", false, null, null, null);
            }
        }

        for (Resident r : residents) {
            if (r.exit() != null) {
                dischargePatient.execute(r.id(), r.exit());
            }
        }
    }

    private void resident(String name, String surname, char sex, LocalDate entry, LocalDate exit, String educator,
                          String psychologist, String substances) {
        int n = residents.size();
        String town = List.of("Valencia", "Torrent", "Sagunto", "Gandia", "Paterna", "Alzira", "Burjassot").get(n % 7);
        PatientDetails details = new PatientDetails(name, surname, new Dni(dni(70000001 + n)),
                LocalDate.of(1970 + (n * 3) % 25, 1 + n % 12, 1 + (n * 7) % 28), "C/ Mayor " + (n + 1) + ", " + town,
                "6601000" + String.format("%02d", n), null, "V-" + String.format("%03d", n + 1), null, substances, null);
        UUID id = admitPatient.execute(details, entry).getId();
        Resident resident = new Resident(id, name, entry, exit, educator, psychologist);
        residents.add(resident);

        assignTutors.execute(id, who(educator), who(psychologist));
        String relative = sex == 'M' ? "Madre" : "Hermana";
        visitors.authorize(id, "Familiar de " + name, relative, "6610000" + String.format("%02d", n));

        // Intervenciones de sus tutores, la última hace poco
        LocalDate last = exit == null ? LocalDate.now(clock.withZone(CENTRE)).minusDays(1 + n % 3) : exit.minusDays(3);
        String[] texts = {
                "Tutoría de acogida: se presentan las normas y se acuerdan los primeros objetivos.",
                "Revisión de objetivos. Buena adaptación a la convivencia.",
                "Sesión familiar con su " + relative.toLowerCase() + ". Se acuerda el régimen de visitas.",
                "Seguimiento semanal: se prepara la próxima salida terapéutica.",
        };
        long days = java.time.temporal.ChronoUnit.DAYS.between(entry, last);
        for (int i = 0; i < texts.length; i++) {
            LocalDate date = entry.plusDays(Math.max(1, days * (i + 1) / texts.length));
            if (date.isAfter(last)) {
                continue;
            }
            recordIntervention.execute(id, who(i % 2 == 0 ? educator : psychologist), date, texts[i]);
        }
    }

    // ------------------------------------------------------------------ Diario y valoraciones

    // Quién trabaja cada parte del día (es también el cuadrante del mes)
    private static final Map<String, Map<ShiftPart, Set<DayOfWeek>>> PATTERN = Map.of(
            "Ana Beltrán", Map.of(ShiftPart.MORNING, EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)),
            "Pablo Ortega", Map.of(ShiftPart.AFTERNOON, EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)),
            "Iván Soler", Map.of(ShiftPart.NIGHT, EnumSet.of(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY)),
            "Rosa Martí", Map.of(ShiftPart.MORNING, EnumSet.of(SATURDAY, SUNDAY), ShiftPart.AFTERNOON,
                    EnumSet.of(SATURDAY, SUNDAY), ShiftPart.NIGHT, EnumSet.of(FRIDAY, SATURDAY)),
            "Carmen López", Map.of(ShiftPart.MORNING, EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)),
            "Laura Gil", Map.of(ShiftPart.MORNING, EnumSet.of(MONDAY, WEDNESDAY, FRIDAY)),
            "Jorge Vidal", Map.of(ShiftPart.MORNING, EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY)),
            "Elena Ríos", Map.of(ShiftPart.AFTERNOON, EnumSet.of(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)),
            "Marta Serra", Map.of(ShiftPart.MORNING, EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)));

    private static final List<String> EDUCATORS = List.of("Ana Beltrán", "Pablo Ortega", "Iván Soler", "Rosa Martí");

    private Set<UUID> onShift(LocalDate day, ShiftPart part) {
        Set<UUID> ids = new java.util.HashSet<>();
        PATTERN.forEach((name, parts) -> {
            if (parts.getOrDefault(part, Set.of()).contains(day.getDayOfWeek())) {
                ids.add(who(name));
            }
        });
        return ids;
    }

    private void diary(LocalDate today) {
        int note = 0;
        for (LocalDate day = today.minusDays(DIARY_DAYS); !day.isAfter(today); day = day.plusDays(1)) {
            // Hoy solo está la mañana: la tarde y la noche se rellenan durante el día
            for (DayPart part : day.equals(today) ? List.of(DayPart.MORNING) : List.of(DayPart.values())) {
                ShiftPart shiftPart = ShiftPart.valueOf(part.name());
                // En el diario figuran los educadores del turno; el resto del personal está en el cuadrante
                Set<UUID> people = onShift(day, shiftPart);
                people.retainAll(EDUCATORS.stream().map(this::who).toList());
                List<String> notes = part == DayPart.MORNING ? DemoTexts.MORNING_NOTES
                        : part == DayPart.AFTERNOON ? DemoTexts.AFTERNOON_NOTES : DemoTexts.NIGHT_NOTES;
                UUID editor = people.stream().findFirst().orElse(who("Marta Serra"));
                recordShift.execute(day, part, people, notes.get(note++ % notes.size()), editor, null);
            }
            activities(day, today);
        }

        // Hoy: dos rebajas (las anota el equipo médico) y una salida
        recordReduction.execute(today, residents.get(1).id(), "Solo tareas ligeras", false, true, who("Laura Gil"));
        recordReduction.execute(today, residents.get(3).id(), null, true, false, who("Laura Gil"));
        recordOuting.execute(today, residents.get(2).id(), LocalTime.of(10, 30), "Consulta en el centro de salud");
    }

    private void activities(LocalDate day, LocalDate today) {
        DayOfWeek weekday = day.getDayOfWeek();
        List<Object[]> planned = new ArrayList<>();
        if (weekday.getValue() <= 5) {
            planned.add(new Object[]{LocalTime.of(10, 0), ActivityType.OCCUPATIONAL_WORKSHOP, "Taller ocupacional"});
        }
        if (weekday == TUESDAY || weekday == THURSDAY) {
            planned.add(new Object[]{LocalTime.of(12, 0), ActivityType.HEALTH_SCHOOL, "Escuela de salud"});
        }
        if (weekday == MONDAY || weekday == WEDNESDAY || weekday == FRIDAY) {
            planned.add(new Object[]{LocalTime.of(17, 0), ActivityType.THERAPEUTIC_GROUP, "Grupo de prevención de recaídas"});
        }
        if (weekday == WEDNESDAY) {
            planned.add(new Object[]{LocalTime.of(12, 0), ActivityType.LIFE_SKILLS, "Habilidades para la vida"});
        }
        if (weekday == SATURDAY) {
            planned.add(new Object[]{LocalTime.of(11, 0), ActivityType.SPORT, "Fútbol en el polideportivo"});
        }
        if (weekday == SUNDAY) {
            planned.add(new Object[]{LocalTime.of(18, 0), ActivityType.VIDEO_FORUM, "Videoforum"});
        }
        UUID educator = weekday.getValue() >= 6 ? who("Rosa Martí") : who("Ana Beltrán");
        for (Object[] a : planned) {
            ActivityView activity = scheduleActivity.execute(day, (LocalTime) a[0], (ActivityType) a[1], (String) a[2]);
            // Las de hoy se quedan sin pasar lista: es lo que hará el personal durante el día
            if (day.equals(today)) {
                continue;
            }
            Map<UUID, AttendanceStatus> attendance = new HashMap<>();
            Map<UUID, Evaluation> evaluations = new HashMap<>();
            for (Resident r : residents) {
                if (!r.presentOn(day)) {
                    continue;
                }
                boolean absent = random.nextInt(14) == 0;
                attendance.put(r.id(), absent ? AttendanceStatus.EXCUSED_ABSENCE : AttendanceStatus.ATTENDED);
                if (!absent) {
                    evaluations.put(r.id(), new Evaluation(mark(), random.nextInt(3) == 0 ? mark() : null,
                            random.nextInt(3) == 0 ? mark() : null));
                }
            }
            recordAttendance.execute(activity.id(), attendance, educator, null);
            recordEvaluations.execute(activity.id(), evaluations, educator, null);
        }
    }

    // De 5 a 10, a medios puntos
    private BigDecimal mark() {
        return BigDecimal.valueOf(10 + random.nextInt(11), 1).multiply(BigDecimal.valueOf(5));
    }

    // ------------------------------------------------------------------ Avisos y cuadrante

    private void notices() {
        String[] authors = {"Carmen López", "Jorge Vidal", "Laura Gil", "Pablo Ortega", "Marta Serra", "Ana Beltrán"};
        for (int i = 0; i < DemoTexts.NOTICES.size(); i++) {
            publishNotice.execute(who(authors[i]), DemoTexts.NOTICES.get(i));
        }
    }

    private void rota(LocalDate today) {
        LocalDate from = today.withDayOfMonth(1);
        LocalDate to = from.plusMonths(2).minusDays(1);
        PATTERN.forEach((name, parts) -> parts.forEach((part, days) ->
                planRota.fill(who(name), from, to, days, Set.of(part))));

        // Un cambio pendiente: Ana pide a Pablo su tarde del jueves que viene a cambio de su mañana
        LocalDate thursday = today.with(java.time.temporal.TemporalAdjusters.next(THURSDAY));
        List<PlannedShift> that = plannedShifts.findBetween(thursday, thursday);
        PlannedShift mine = that.stream().filter(s -> s.isOf(who("Ana Beltrán")) && s.getPart() == ShiftPart.MORNING)
                .findFirst().orElseThrow();
        PlannedShift theirs = that.stream().filter(s -> s.isOf(who("Pablo Ortega")) && s.getPart() == ShiftPart.AFTERNOON)
                .findFirst().orElseThrow();
        swaps.propose(who("Ana Beltrán"), mine.getId(), who("Pablo Ortega"), theirs.getId());
    }

    // ------------------------------------------------------------------ Informes

    private void reports(LocalDate today) {
        // Lucía: el de media estancia, completo (pendiente de cerrar)
        write(residents.get(0), ReportType.MID_STAY, DemoTexts.MID_STAY, Integer.MAX_VALUE);
        // Nerea: el de media estancia, a medias (áreas social y sanitaria)
        write(residents.get(4), ReportType.MID_STAY, DemoTexts.MID_STAY, 11);
        // Óscar: el final, completo y cerrado por la administración
        Resident oscar = residents.get(10);
        Report finalReport = report(oscar, ReportType.FINAL);
        editHeader.execute(finalReport.getId(), "UCA-2025-0731", "UCA de Gandia", null,
                "Alta terapéutica: ha completado el programa.", null, finalReport.getHeaderVersion(), who("Marta Serra"));
        write(oscar, ReportType.FINAL, DemoTexts.FINAL, Integer.MAX_VALUE);
        closeReport.close(finalReport.getId(), who("Marta Serra"));
    }

    private Report report(Resident resident, ReportType type) {
        return reportRepository.findByPatient(resident.id()).stream()
                .filter(r -> r.getType() == type && r.getStayStart().equals(resident.entry()))
                .findFirst().orElseThrow();
    }

    // Cada área la redacta su especialista; las de los tutores, su psicólogo (o la administración si ya no lo es)
    private void write(Resident resident, ReportType type, Map<ReportSection, String> texts, int limit) {
        Report report = report(resident, type);
        int written = 0;
        for (ReportSection section : ReportSection.of(type)) {
            if (written++ >= limit) {
                return;
            }
            writeSection.execute(report.getId(), section, texts.get(section), 0, writer(resident, section.area()));
        }
    }

    private UUID writer(Resident resident, ReportArea area) {
        return switch (area) {
            case SOCIAL, FAMILY -> who("Carmen López");
            case HEALTH -> who("Laura Gil");
            case PSYCHOLOGICAL -> who(resident.psychologist());
            case EDUCATIONAL -> who(resident.educator());
            case DURATION, FOLLOW_UP -> resident.exit() == null ? who(resident.psychologist()) : who("Marta Serra");
        };
    }

    // ------------------------------------------------------------------ Utilidades

    // Un DNI válido: el número y su letra de control
    private static String dni(int number) {
        return number + String.valueOf("TRWAGMYFPDXBNJZSQVHLCKE".charAt(number % 23));
    }

    // Una firma dibujada a mano alzada (curvas, sin fuentes: el contenedor no tiene) distinta para cada nombre
    private static byte[] signature(String name) {
        BufferedImage image = new BufferedImage(240, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1d, 0x3c, 0x6e));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Random shape = new Random(name.hashCode());
        Path2D path = new Path2D.Double();
        double x = 14;
        double y = 50;
        path.moveTo(x, y);
        for (int i = 0; i < 6; i++) {
            double nx = x + 22 + shape.nextInt(14);
            path.curveTo(x + 6, 12 + shape.nextInt(16), nx - 6, 64 - shape.nextInt(10), nx, 42 + shape.nextInt(14));
            x = nx;
        }
        path.quadTo(x + 20, 30, x + 36, 46);
        g.draw(path);
        g.drawLine(18, 66, (int) x + 30, 62);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
