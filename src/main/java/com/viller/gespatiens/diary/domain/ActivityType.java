package com.viller.gespatiens.diary.domain;

// Tipos de actividad de la memoria (tablas Lessons, Therapeutic_Groups, Sports y Walks) más "Otros",
// que pidió el centro para talleres externos o actividades fuera de lo común
public enum ActivityType {
    THERAPEUTIC_GROUP,
    LIFE_SKILLS,
    HEALTH_SCHOOL,
    JOB_GUIDANCE,
    OCCUPATIONAL_WORKSHOP,
    VIDEO_FORUM,
    MAINTENANCE,
    SPORT,
    WALK,
    OTHER;

    // Un grupo terapéutico necesita el nombre del grupo; "Otros", qué es
    public boolean requiresTitle() {
        return this == THERAPEUTIC_GROUP || this == OTHER;
    }

    // Las actividades de las que libra una rebaja deportiva
    public boolean isPhysical() {
        return this == SPORT || this == WALK;
    }
}
