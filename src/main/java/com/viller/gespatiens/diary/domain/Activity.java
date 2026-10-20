package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Una actividad programada en el diario y su lista de asistencia. La memoria: "Se podrán apuntar los
// usuarios asistentes a estas actividades, así como los usuarios que tienen permiso para no presentarse
// (rebajados), y los que no han realizado la actividad, dividiendo a estos entre justificados y no justificados".
// También guarda la valoración (RF6) de quienes asistieron
public class Activity {

    public static final int MAX_TITLE_LENGTH = 100;

    private final UUID id;
    private final LocalDate date;
    private LocalTime startTime;
    private ActivityType type;
    private String title;
    private Map<UUID, AttendanceStatus> attendance;
    private Map<UUID, Evaluation> evaluations;

    private Activity(UUID id, LocalDate date, LocalTime startTime, ActivityType type, String title,
                     Map<UUID, AttendanceStatus> attendance, Map<UUID, Evaluation> evaluations) {
        if (id == null || date == null) {
            throw new IllegalArgumentException("La actividad necesita id y fecha");
        }
        this.id = id;
        this.date = date;
        describe(startTime, type, title);
        this.attendance = Map.copyOf(attendance == null ? Map.of() : attendance);
        this.evaluations = Map.of();
        evaluate(evaluations);
    }

    // Las actividades se programan día a día, también con antelación
    public static Activity schedule(LocalDate date, LocalTime startTime, ActivityType type, String title) {
        return new Activity(UUID.randomUUID(), date, startTime, type, title, Map.of(), Map.of());
    }

    public static Activity reconstitute(UUID id, LocalDate date, LocalTime startTime, ActivityType type, String title,
                                        Map<UUID, AttendanceStatus> attendance,
                                        Map<UUID, Evaluation> evaluations) {
        return new Activity(id, date, startTime, type, title, attendance, evaluations);
    }

    public void reschedule(LocalTime newStartTime, ActivityType newType, String newTitle) {
        describe(newStartTime, newType, newTitle);
    }

    // Pasar lista: sustituye la lista entera. Solo de actividades de hoy o anteriores. Quien deja de
    // figurar como asistente pierde su valoración
    public void recordAttendance(Map<UUID, AttendanceStatus> newAttendance, LocalDate today) {
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("No se puede pasar lista de una actividad que todavía no ha llegado");
        }
        this.attendance = Map.copyOf(newAttendance == null ? Map.of() : newAttendance);
        Map<UUID, Evaluation> kept = new HashMap<>(evaluations);
        kept.keySet().removeIf(patientId -> !attended(patientId));
        this.evaluations = Map.copyOf(kept);
    }

    // Valorar: sustituye todas las valoraciones. Solo se valora a quien asistió
    public void recordEvaluations(Map<UUID, Evaluation> newEvaluations) {
        evaluate(newEvaluations);
    }

    public boolean attended(UUID patientId) {
        return attendance.get(patientId) == AttendanceStatus.ATTENDED;
    }

    private void evaluate(Map<UUID, Evaluation> newEvaluations) {
        Map<UUID, Evaluation> checked = newEvaluations == null ? Map.of() : newEvaluations;
        if (!checked.keySet().stream().allMatch(this::attended)) {
            throw new IllegalArgumentException("Solo se puede valorar a quien asistió a la actividad");
        }
        this.evaluations = Map.copyOf(checked);
    }

    private void describe(LocalTime startTime, ActivityType type, String title) {
        if (startTime == null) {
            throw new IllegalArgumentException("Indica la hora de la actividad");
        }
        if (type == null) {
            throw new IllegalArgumentException("Indica el tipo de actividad");
        }
        String cleaned = title == null || title.isBlank() ? null : title.strip();
        if (cleaned == null && type.requiresTitle()) {
            throw new IllegalArgumentException(type == ActivityType.OTHER
                    ? "Indica qué actividad es"
                    : "Indica el nombre del grupo terapéutico");
        }
        if (cleaned != null && cleaned.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre de la actividad no puede superar los " + MAX_TITLE_LENGTH + " caracteres");
        }
        this.startTime = startTime;
        this.type = type;
        this.title = cleaned;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public ActivityType getType() {
        return type;
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public Map<UUID, AttendanceStatus> getAttendance() {
        return attendance;
    }

    public Map<UUID, Evaluation> getEvaluations() {
        return evaluations;
    }
}
