-- El tema de la aplicación que ha elegido cada empleado. Los que ya existían, el de su equipo
ALTER TABLE users ADD COLUMN theme VARCHAR(6) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE users ADD CONSTRAINT ck_users_theme CHECK (theme IN ('SYSTEM', 'LIGHT', 'DARK'));
