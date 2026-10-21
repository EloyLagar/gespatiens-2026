package com.viller.gespatiens.diary.domain;

import java.math.BigDecimal;

// La valoración de un residente en una actividad (RF6): la nota que le pone el personal y la satisfacción
// y la utilidad que él da a la actividad. Los residentes no usan la aplicación: las dan en papel y las
// pasa el educador. Las tres de 0 a 10 con un decimal como mucho; basta con una (las demás, null)
public record Evaluation(BigDecimal mark, BigDecimal satisfaction, BigDecimal utility) {

    public static final BigDecimal MIN = BigDecimal.ZERO;
    public static final BigDecimal MAX = BigDecimal.TEN;

    public Evaluation {
        if (mark == null && satisfaction == null && utility == null) {
            throw new IllegalArgumentException("Indica la nota, la satisfacción o la utilidad");
        }
        mark = checked(mark, "La nota");
        satisfaction = checked(satisfaction, "La satisfacción");
        utility = checked(utility, "La utilidad");
    }

    private static BigDecimal checked(BigDecimal value, String what) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
            throw new IllegalArgumentException(what + " va de 0 a 10");
        }
        BigDecimal plain = value.stripTrailingZeros();
        if (plain.scale() > 1) {
            throw new IllegalArgumentException(what + " admite un decimal como mucho");
        }
        // Misma escala siempre (7 -> 7.0) para que dos valoraciones iguales sean equals
        return plain.setScale(1);
    }
}
