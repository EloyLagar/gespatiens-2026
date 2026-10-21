package com.viller.gespatiens.patients.domain;

// Value object: un DNI (12345678Z) o NIE (X1234567L) con su letra de control verificada.
// Si existe un objeto Dni, es válido: nadie puede crear uno incorrecto.
public record Dni(String value) {

    private static final String CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    public Dni {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
        value = value.trim().toUpperCase();
        if (!value.matches("^[XYZ0-9][0-9]{7}[A-Z]$")) {
            throw new IllegalArgumentException("Formato de DNI/NIE inválido");
        }
        // En el NIE la letra inicial cuenta como dígito: X=0, Y=1, Z=2
        String digits = switch (value.charAt(0)) {
            case 'X' -> "0" + value.substring(1, 8);
            case 'Y' -> "1" + value.substring(1, 8);
            case 'Z' -> "2" + value.substring(1, 8);
            default -> value.substring(0, 8);
        };
        char expectedLetter = CONTROL_LETTERS.charAt(Integer.parseInt(digits) % 23);
        if (value.charAt(8) != expectedLetter) {
            throw new IllegalArgumentException("La letra del DNI/NIE no es correcta");
        }
    }
}
