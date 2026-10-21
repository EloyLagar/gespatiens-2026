-- Memoria final: "Al generar un perfil de empleado, llegará un correo [...] Desde allí, el empleado podrá crear
-- sus credenciales de acceso". Hasta que lo haga, el empleado no tiene contraseña
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Los enlaces del correo. Se guarda el hash SHA-256 del token, nunca el token: con la base de datos
-- en la mano no se puede usar un enlace vigente
CREATE TABLE password_setup_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id    UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_password_setup_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Un enlace nuevo anula los anteriores del mismo empleado
CREATE INDEX ix_password_setup_tokens_user ON password_setup_tokens (user_id);
