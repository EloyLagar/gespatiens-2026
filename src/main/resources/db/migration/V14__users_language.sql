-- RNF9: el idioma de la aplicación que ha elegido cada empleado. Los que ya existían, en español
ALTER TABLE users ADD COLUMN language VARCHAR(2) NOT NULL DEFAULT 'ES';
ALTER TABLE users ADD CONSTRAINT ck_users_language CHECK (language IN ('ES', 'EN'));
