-- RF8: "si algún tipo de información estaba siendo modificada, no se debía dejar acceder a esa información
-- a otros empleados". Quién está editando qué (un apartado de un informe, un turno, una actividad...).
-- Cada bloqueo caduca solo si quien lo tiene deja de renovarlo: nunca se queda un documento inaccesible
CREATE TABLE edit_locks (
    -- Lo que se edita, p. ej. "report:<id>:section:PSYCHO_DIAGNOSIS" o "shift:2026-10-21:MORNING"
    resource    VARCHAR(150) PRIMARY KEY,
    holder_id   UUID         NOT NULL,
    acquired_at TIMESTAMPTZ  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    -- Bloqueo optimista: dos empleados no pueden quedarse a la vez con un bloqueo caducado
    revision    BIGINT       NOT NULL,

    CONSTRAINT fk_edit_locks_holder FOREIGN KEY (holder_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_edit_locks_expiry CHECK (expires_at > acquired_at)
);
