package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class ActivityJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @Column(length = 100)
    private String title;

    // residente -> asistencia. EAGER: el adaptador traduce a dominio fuera de la transacción
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_attendance", joinColumns = @JoinColumn(name = "activity_id"))
    @MapKeyColumn(name = "patient_id")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Map<UUID, AttendanceStatus> attendance = new HashMap<>();

    // residente -> su valoración. Son Map y no List, así que dos colecciones EAGER no dan problemas
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_evaluations", joinColumns = @JoinColumn(name = "activity_id"))
    @MapKeyColumn(name = "patient_id")
    private Map<UUID, EvaluationEmbeddable> evaluations = new HashMap<>();

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

    public String getTitle() {
        return title;
    }

    public Map<UUID, AttendanceStatus> getAttendance() {
        return attendance;
    }

    public Map<UUID, EvaluationEmbeddable> getEvaluations() {
        return evaluations;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAttendance(Map<UUID, AttendanceStatus> attendance) {
        this.attendance = new HashMap<>(attendance);
    }

    public void setEvaluations(Map<UUID, EvaluationEmbeddable> evaluations) {
        this.evaluations = new HashMap<>(evaluations);
    }
}
