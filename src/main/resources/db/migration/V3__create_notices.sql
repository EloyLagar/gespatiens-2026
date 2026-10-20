-- Avisos entre el personal del centro (agregado Notice del módulo notices, RF5)
CREATE TABLE notices (
    id         UUID          PRIMARY KEY,
    -- NULL si el autor ya no está en la plantilla: el aviso se conserva aunque se borre al empleado
    author_id  UUID,
    text       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    -- NULL mientras no se haya modificado
    edited_at  TIMESTAMPTZ,

    CONSTRAINT fk_notices_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_notices_text_not_blank CHECK (length(trim(text)) > 0),
    CONSTRAINT ck_notices_edited_after_created CHECK (edited_at IS NULL OR edited_at >= created_at)
);

-- La pantalla de inicio los lista del más reciente al más antiguo
CREATE INDEX ix_notices_created_at ON notices (created_at DESC);
