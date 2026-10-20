-- Diario (memoria, apartado 4.1): turnos de mañana, tarde y noche, y rebajas del equipo médico

-- Un registro por día y turno: quién trabajó y la información de interés
CREATE TABLE shift_logs (
    id         UUID          PRIMARY KEY,
    date       DATE          NOT NULL,
    day_part   VARCHAR(10)   NOT NULL,
    notes      VARCHAR(2000),
    -- Quién lo rellenó por última vez. NULL si ese empleado ya no está en la plantilla
    updated_by UUID,
    updated_at TIMESTAMPTZ   NOT NULL,

    CONSTRAINT uk_shift_logs_date_part UNIQUE (date, day_part),
    CONSTRAINT ck_shift_logs_day_part CHECK (day_part IN ('MORNING', 'AFTERNOON', 'NIGHT')),
    CONSTRAINT fk_shift_logs_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL
);

-- Los empleados que trabajaron en cada turno (Works_in en la memoria)
CREATE TABLE shift_staff (
    shift_log_id UUID NOT NULL,
    user_id      UUID NOT NULL,

    CONSTRAINT pk_shift_staff PRIMARY KEY (shift_log_id, user_id),
    CONSTRAINT fk_shift_staff_log FOREIGN KEY (shift_log_id) REFERENCES shift_logs (id) ON DELETE CASCADE,
    CONSTRAINT fk_shift_staff_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Residentes rebajados un día concreto (Reductions en la memoria). Solo el equipo médico las registra
CREATE TABLE reductions (
    id                UUID         PRIMARY KEY,
    date              DATE         NOT NULL,
    patient_id        UUID         NOT NULL,
    -- Rebaja parcial: de qué está rebajado ("no puede cargar peso", "sin actividades al aire libre")
    partial_reduction VARCHAR(100),
    total_reduction   BOOLEAN      NOT NULL,
    sport_reduction   BOOLEAN      NOT NULL,
    recorded_by       UUID,
    recorded_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_reductions_date_patient UNIQUE (date, patient_id),
    -- Misma invariante que Reduction: una rebaja tiene al menos un tipo
    CONSTRAINT ck_reductions_some_type CHECK (partial_reduction IS NOT NULL OR total_reduction OR sport_reduction),
    -- Borrar definitivamente la ficha de un antiguo residente borra también sus rebajas
    CONSTRAINT fk_reductions_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_reductions_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id) ON DELETE SET NULL
);
