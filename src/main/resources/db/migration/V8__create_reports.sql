-- Informes de media estancia y de fin de programa (Reports, Mid_stay_reports, Final_reports y Writes_about
-- en la memoria). Los apartados de cada informe los define la aplicación (ReportSection): aquí se guarda
-- una fila por apartado redactado, con quién lo escribió por última vez, en lugar de una columna por apartado

CREATE TABLE reports (
    id               UUID         PRIMARY KEY,
    patient_id       UUID         NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    -- RF8: un informe cerrado ya no se modifica hasta que la administración lo reabra
    state            VARCHAR(20)  NOT NULL,
    -- Cabecera del informe
    request_number   VARCHAR(30),
    reference_center VARCHAR(150),
    nip              VARCHAR(30),
    entry_date       DATE,
    exit_date        DATE,
    exit_reason      VARCHAR(200),
    -- Cuántas veces se ha modificado la cabecera: quien la guarda dice qué versión tenía delante
    header_version   INTEGER      NOT NULL,
    -- NULL si quien lo empezó ya no está en la plantilla
    created_by       UUID,
    created_at       TIMESTAMPTZ  NOT NULL,
    closed_by        UUID,
    closed_at        TIMESTAMPTZ,
    -- Bloqueo optimista de JPA: dos guardados simultáneos del mismo informe no se pisan
    revision         BIGINT       NOT NULL,

    CONSTRAINT fk_reports_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_closed_by FOREIGN KEY (closed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_reports_type CHECK (type IN ('MID_STAY', 'FINAL')),
    CONSTRAINT ck_reports_state CHECK (state IN ('DRAFT', 'CLOSED')),
    CONSTRAINT ck_reports_closed CHECK ((state = 'CLOSED') = (closed_at IS NOT NULL)),
    CONSTRAINT ck_reports_dates CHECK (exit_date IS NULL OR entry_date IS NULL OR exit_date >= entry_date)
);

-- La ficha de un residente lista sus informes por tipo
CREATE INDEX ix_reports_patient_type ON reports (patient_id, type, created_at);

-- Un residente no puede tener dos informes del mismo tipo en elaboración a la vez
CREATE UNIQUE INDEX ux_reports_one_draft_per_type ON reports (patient_id, type) WHERE state = 'DRAFT';

-- Cada apartado redactado de un informe y quién lo escribió por última vez (Writes_about en la memoria)
CREATE TABLE report_sections (
    report_id  UUID          NOT NULL,
    section    VARCHAR(60)   NOT NULL,
    text       VARCHAR(4000) NOT NULL,
    -- NULL si el autor ya no está en la plantilla: lo escrito se conserva
    author_id  UUID,
    updated_at TIMESTAMPTZ   NOT NULL,
    -- RF8: cuántas veces se ha guardado el apartado. Si alguien lo cambió mientras otro lo editaba,
    -- el segundo guardado se rechaza en lugar de borrar lo que escribió el primero
    version    INTEGER       NOT NULL,

    CONSTRAINT pk_report_sections PRIMARY KEY (report_id, section),
    CONSTRAINT fk_report_sections_report FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_sections_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_report_sections_text_not_blank CHECK (length(trim(text)) > 0),
    CONSTRAINT ck_report_sections_version CHECK (version > 0)
);
