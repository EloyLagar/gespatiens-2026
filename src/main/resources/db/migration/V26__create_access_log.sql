-- Registro de accesos (ENS op.acc y op.exp.8): cada intento de entrar en la aplicación, haya ido bien o mal.
-- email es el que se escribió, exista o no; user_id, el empleado de ese email si existía. Sin claves ajenas a
-- propósito: si se borra al empleado, sus entradas se conservan hasta que pase el plazo (2 años)
CREATE TABLE access_log (
    id          UUID         PRIMARY KEY,
    occurred_at TIMESTAMPTZ  NOT NULL,
    email       VARCHAR(254) NOT NULL,
    user_id     UUID,
    outcome     VARCHAR(20)  NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(255)
);

-- Las consultas de la pantalla: todo, lo de un empleado y los fallos, de lo más reciente atrás
CREATE INDEX ix_access_log_occurred_at ON access_log (occurred_at DESC);
CREATE INDEX ix_access_log_user ON access_log (user_id, occurred_at DESC);
CREATE INDEX ix_access_log_outcome ON access_log (outcome, occurred_at DESC);
