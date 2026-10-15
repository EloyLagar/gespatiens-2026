-- Residentes del centro (agregado Patient del módulo patients)
CREATE TABLE patients (
    id               UUID         PRIMARY KEY,
    -- Número de referencia 1-32 (RF11). NULL en los residentes dados de baja;
    -- PostgreSQL permite varios NULL en una columna UNIQUE
    number           INTEGER,
    name             VARCHAR(50)  NOT NULL,
    surname          VARCHAR(100) NOT NULL,
    dni              VARCHAR(9)   NOT NULL,
    birth_date       DATE         NOT NULL,
    address          VARCHAR(500) NOT NULL,
    phone_number     VARCHAR(20)  NOT NULL,
    sip              VARCHAR(25),
    visit_code       VARCHAR(25),
    belongings       TEXT,
    abuse_substances VARCHAR(100),
    extra_info       TEXT,
    entry_date       DATE         NOT NULL,
    exit_date        DATE,

    CONSTRAINT uk_patients_dni        UNIQUE (dni),
    CONSTRAINT uk_patients_number     UNIQUE (number),
    CONSTRAINT uk_patients_visit_code UNIQUE (visit_code),
    CONSTRAINT ck_patients_number     CHECK (number BETWEEN 1 AND 32),
    -- Misma invariante que Patient.validateState: activo <=> tiene número y no tiene fecha de salida
    CONSTRAINT ck_patients_active_state CHECK (
        (exit_date IS NULL AND number IS NOT NULL) OR
        (exit_date IS NOT NULL AND number IS NULL AND exit_date >= entry_date))
);
