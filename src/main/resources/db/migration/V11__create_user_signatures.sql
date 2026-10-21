-- Memoria final: "La aplicación solicitará también que el empleado por cuenta propia suba una imagen de su
-- firma la cual se empleará para los informes". La firma escaneada, en PNG
CREATE TABLE user_signatures (
    user_id     UUID        PRIMARY KEY,
    image       BYTEA       NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_user_signatures_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
