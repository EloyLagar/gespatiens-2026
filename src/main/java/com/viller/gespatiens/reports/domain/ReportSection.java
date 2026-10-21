package com.viller.gespatiens.reports.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.viller.gespatiens.reports.domain.ReportType.FINAL;
import static com.viller.gespatiens.reports.domain.ReportType.MID_STAY;

// Los apartados de cada informe, con el texto y el orden de los modelos del centro (memoria, figuras 9 y 10).
// En la base de datos se guarda el nombre de la constante: no se puede renombrar sin una migración
public enum ReportSection {

    // Área social
    SOCIAL_FAMILY_SITUATION(ReportArea.SOCIAL, "Situación socio-familiar", MID_STAY, FINAL),
    WORK_EDUCATION_ECONOMIC_SITUATION(ReportArea.SOCIAL, "Situación laboral, formativa y económica", MID_STAY),
    WORK_EDUCATION_SITUATION(ReportArea.SOCIAL, "Situación laboral, formativa", FINAL),
    JUDICIAL_SITUATION(ReportArea.SOCIAL, "Situación judicial", MID_STAY, FINAL),
    SOCIAL_EVOLUTION(ReportArea.SOCIAL, "Evolución y objetivos alcanzados", FINAL),
    SOCIAL_DIAGNOSIS(ReportArea.SOCIAL, "Diagnóstico social (Eje IV en DSM IV-R)", MID_STAY, FINAL),
    SOCIAL_OBJECTIVES(ReportArea.SOCIAL, "Objetivos planteados", MID_STAY),

    // Área de intervención sanitaria
    TOXICOLOGICAL_HISTORY(ReportArea.HEALTH, "Resumen historia toxicológica", FINAL),
    HEALTH_AT_ENTRY(ReportArea.HEALTH, "Situación sanitaria al ingreso", MID_STAY, FINAL),
    DETOX_AT_ENTRY(ReportArea.HEALTH, "Controles toxicológicos y estado de desintoxicación al ingreso", MID_STAY),
    HEALTH_DIAGNOSIS_MID_STAY(ReportArea.HEALTH, "Diagnóstico (Eje III en DSM IV)", MID_STAY),
    HEALTH_EVOLUTION(ReportArea.HEALTH,
            "Valoración de la evolución del estado físico-sanitario. Atenciones sanitarias", MID_STAY),
    HEALTH_EVOLUTION_DURING_PROGRAM(ReportArea.HEALTH,
            "Evolución del estado físico-sanitario durante el programa", FINAL),
    TOXICOLOGICAL_CONTROLS(ReportArea.HEALTH, "Analíticas y controles toxicológicos", MID_STAY),
    TOXICOLOGICAL_CONTROLS_DURING_PROGRAM(ReportArea.HEALTH,
            "Analíticas y controles toxicológicos durante el programa", FINAL),
    OTHER_MEDICAL_CARE(ReportArea.HEALTH, "Otras atenciones médico-sanitarias en programa", FINAL),
    HEALTH_DIAGNOSIS_FINAL(ReportArea.HEALTH, "Diagnóstico (Eje III en DSM IV)", FINAL),
    HEALTH_OBJECTIVES(ReportArea.HEALTH, "Objetivos planteados", MID_STAY),
    HEALTH_AT_DISCHARGE(ReportArea.HEALTH, "Situación y tratamiento al alta", FINAL),

    // Área de intervención psicológica
    PSYCHO_ENTRY_ASSESSMENT(ReportArea.PSYCHOLOGICAL,
            "Valoración inicial. Motivo de ingreso (según el propio usuario)", MID_STAY),
    PSYCHO_INITIAL_ASSESSMENT(ReportArea.PSYCHOLOGICAL, "Valoración inicial", FINAL),
    MOTIVATION_MID_STAY(ReportArea.PSYCHOLOGICAL,
            "Valoración de la motivación (hacia el programa, hacia la abstinencia y al cambio de estilo de vida)",
            MID_STAY),
    PSYCHO_EVALUATION_MID_STAY(ReportArea.PSYCHOLOGICAL,
            "Evaluación psicológica: instrumentos, pruebas exploratorias, análisis y conclusiones del caso, "
                    + "diagnóstico dual", MID_STAY),
    PSYCHO_EVALUATION_FINAL(ReportArea.PSYCHOLOGICAL,
            "Evaluación psicológica: instrumentos, pruebas exploratorias, diagnóstico dual. "
                    + "Conclusiones de la evaluación", FINAL),
    PSYCHO_DIAGNOSIS(ReportArea.PSYCHOLOGICAL, "Diagnóstico (Eje I y II en DSM-IV)", MID_STAY, FINAL),
    PSYCHO_INTERVENTION(ReportArea.PSYCHOLOGICAL, "Intervención psicológica individual requerida", MID_STAY),
    PSYCHO_OBJECTIVES(ReportArea.PSYCHOLOGICAL, "Objetivos planteados", MID_STAY),
    PSYCHO_INTERVENTION_EVOLUTION(ReportArea.PSYCHOLOGICAL, "Intervención y objetivos: evolución", FINAL),
    MOTIVATION_FINAL(ReportArea.PSYCHOLOGICAL,
            "Motivación (hacia la abstinencia y al cambio de estilo de vida)", FINAL),
    PSYCHO_OUTINGS(ReportArea.PSYCHOLOGICAL, "Desarrollo y valoración de las Salidas Terapéuticas", FINAL),
    PSYCHO_AT_DISCHARGE(ReportArea.PSYCHOLOGICAL, "Situación al alta", FINAL),

    // Área educativa. Los cinco primeros cuelgan de "Grado de adaptación al centro y de implicación..."
    ADAPTATION_HABITS(ReportArea.EDUCATIONAL, "En cuanto a hábitos", true, MID_STAY, FINAL),
    ADAPTATION_RESPONSIBILITIES(ReportArea.EDUCATIONAL, "En cuanto a actividades-responsabilidades", true,
            MID_STAY, FINAL),
    ADAPTATION_RULES(ReportArea.EDUCATIONAL, "En cuanto a normatividad", true, MID_STAY, FINAL),
    ADAPTATION_WORKSHOPS(ReportArea.EDUCATIONAL, "En cuanto a talleres o formación", true, MID_STAY, FINAL),
    ADAPTATION_LEISURE(ReportArea.EDUCATIONAL, "En cuanto a ocio y tiempo libre", true, MID_STAY, FINAL),
    PEER_RELATIONSHIP(ReportArea.EDUCATIONAL, "Relación con los compañeros", MID_STAY, FINAL),
    TEAM_RELATIONSHIP(ReportArea.EDUCATIONAL, "Relación con el equipo terapéutico", MID_STAY, FINAL),
    EDUCATIONAL_OBJECTIVES(ReportArea.EDUCATIONAL, "Objetivos planteados", MID_STAY),
    EDUCATIONAL_OUTINGS(ReportArea.EDUCATIONAL, "Desarrollo y valoración de las Salidas Terapéuticas", FINAL),

    // Área de intervención familiar
    REFERENCE_RELATIVES(ReportArea.FAMILY,
            "Familiares de referencia en el programa y grado de implicación de los mismos", MID_STAY, FINAL),
    FAMILY_RELATIONSHIP(ReportArea.FAMILY, "Relación con la familia y evolución", MID_STAY, FINAL),
    FAMILY_AT_DISCHARGE(ReportArea.FAMILY, "Situación al alta", FINAL),

    // Previsión de la duración del programa (en meses)
    PROGRAM_DURATION_FORECAST(ReportArea.DURATION, "Previsión de la duración del programa (en meses)", MID_STAY),

    // Orientaciones de seguimiento para la unidad de referencia
    DISCHARGE_GROUNDS(ReportArea.FOLLOW_UP, "Fundamento de la salida", FINAL),
    AFTER_DISCHARGE_OBJECTIVES(ReportArea.FOLLOW_UP,
            "Objetivos de intervención tras finalización del programa en el centro", FINAL);

    // El encabezado de los apartados de adaptación en el área educativa
    public static final String ADAPTATION_HEADING =
            "Grado de adaptación al centro y de implicación en los diferentes componentes de esta área:";

    private final ReportArea area;
    private final String label;
    private final boolean adaptation;
    private final Set<ReportType> types;

    ReportSection(ReportArea area, String label, ReportType... types) {
        this(area, label, false, types);
    }

    ReportSection(ReportArea area, String label, boolean adaptation, ReportType... types) {
        this.area = area;
        this.label = label;
        this.adaptation = adaptation;
        this.types = EnumSet.copyOf(Arrays.asList(types));
    }

    // Los apartados de un tipo de informe, en orden
    public static List<ReportSection> of(ReportType type) {
        return Arrays.stream(values()).filter(section -> section.belongsTo(type)).toList();
    }

    public boolean belongsTo(ReportType type) {
        return types.contains(type);
    }

    public ReportArea area() {
        return area;
    }

    public String label() {
        return label;
    }

    // Va dentro del "Grado de adaptación al centro" del área educativa
    public boolean adaptation() {
        return adaptation;
    }
}
