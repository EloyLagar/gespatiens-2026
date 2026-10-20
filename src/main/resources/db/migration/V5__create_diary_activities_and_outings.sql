-- Diario, segunda parte: actividades programadas con su asistencia, y salidas terapéuticas

-- Actividades de un día (Activities en la memoria). Se programan día a día, no hay horario fijo
CREATE TABLE activities (
    id         UUID         PRIMARY KEY,
    date       DATE         NOT NULL,
    start_time TIME         NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    -- El nombre del grupo terapéutico, o qué es una actividad de tipo "Otros"
    title      VARCHAR(100),

    CONSTRAINT ck_activities_type CHECK (type IN ('THERAPEUTIC_GROUP', 'LIFE_SKILLS', 'HEALTH_SCHOOL',
        'JOB_GUIDANCE', 'OCCUPATIONAL_WORKSHOP', 'VIDEO_FORUM', 'MAINTENANCE', 'SPORT', 'WALK', 'OTHER')),
    -- Misma invariante que Activity: los grupos terapéuticos y "Otros" necesitan nombre
    CONSTRAINT ck_activities_title CHECK (type NOT IN ('THERAPEUTIC_GROUP', 'OTHER') OR title IS NOT NULL)
);

CREATE INDEX ix_activities_date ON activities (date, start_time);

-- Asistencia de cada residente a una actividad (Attendance en la memoria).
-- "Rebajado" no se guarda: sale de las rebajas de ese día
CREATE TABLE activity_attendance (
    activity_id UUID        NOT NULL,
    patient_id  UUID        NOT NULL,
    status      VARCHAR(20) NOT NULL,

    CONSTRAINT pk_activity_attendance PRIMARY KEY (activity_id, patient_id),
    CONSTRAINT ck_activity_attendance_status CHECK (status IN ('ATTENDED', 'EXCUSED_ABSENCE', 'UNEXCUSED_ABSENCE')),
    CONSTRAINT fk_activity_attendance_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_attendance_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);

-- Salidas terapéuticas: cada residente que sale del centro, cuándo sale y cuándo vuelve
CREATE TABLE therapeutic_outings (
    id          UUID         PRIMARY KEY,
    date        DATE         NOT NULL,
    patient_id  UUID         NOT NULL,
    left_at     TIME         NOT NULL,
    -- NULL mientras no ha vuelto
    returned_at TIME,
    notes       VARCHAR(200),

    CONSTRAINT ck_therapeutic_outings_return CHECK (returned_at IS NULL OR returned_at >= left_at),
    CONSTRAINT fk_therapeutic_outings_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);

CREATE INDEX ix_therapeutic_outings_date ON therapeutic_outings (date);
