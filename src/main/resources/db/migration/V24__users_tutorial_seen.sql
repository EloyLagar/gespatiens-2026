-- El tutorial de bienvenida sale la primera vez que entra cada empleado. Se guarda aquí y no en el
-- navegador para que no le vuelva a salir en otro equipo. Los que ya existían todavía no lo han visto
ALTER TABLE users ADD COLUMN tutorial_seen BOOLEAN NOT NULL DEFAULT FALSE;
