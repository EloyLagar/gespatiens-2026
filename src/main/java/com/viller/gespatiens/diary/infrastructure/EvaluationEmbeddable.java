package com.viller.gespatiens.diary.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

// Una fila de activity_evaluations sin sus claves (las pone ActivityJpaEntity)
@Embeddable
public class EvaluationEmbeddable {

    @Column(precision = 3, scale = 1)
    private BigDecimal mark;

    @Column(precision = 3, scale = 1)
    private BigDecimal satisfaction;

    @Column(precision = 3, scale = 1)
    private BigDecimal utility;

    protected EvaluationEmbeddable() {
    }

    public EvaluationEmbeddable(BigDecimal mark, BigDecimal satisfaction, BigDecimal utility) {
        this.mark = mark;
        this.satisfaction = satisfaction;
        this.utility = utility;
    }

    public BigDecimal getMark() {
        return mark;
    }

    public BigDecimal getSatisfaction() {
        return satisfaction;
    }

    public BigDecimal getUtility() {
        return utility;
    }
}
