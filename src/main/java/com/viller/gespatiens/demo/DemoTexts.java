package com.viller.gespatiens.demo;

import com.viller.gespatiens.reports.domain.ReportSection;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Los textos inventados de los informes y del diario de la demostración. Ningún dato es de una persona real
final class DemoTexts {

    private DemoTexts() {
    }

    // Informe de media estancia de una residente (Lucía Martínez Soler)
    static final Map<ReportSection, String> MID_STAY = new EnumMap<>(Map.ofEntries(
            Map.entry(ReportSection.SOCIAL_FAMILY_SITUATION, "Tiene 38 años, está separada y tiene una hija de 12 años que vive con el padre en Valencia. Antes del ingreso residía con su madre en Sagunto, que es su principal apoyo."),
            Map.entry(ReportSection.WORK_EDUCATION_ECONOMIC_SITUATION, "Estudios de técnica en cuidados auxiliares de enfermería. Trabajó en una residencia de mayores hasta 2025. Percibe el subsidio por desempleo hasta mayo de 2027."),
            Map.entry(ReportSection.JUDICIAL_SITUATION, "Sin antecedentes penales ni causas pendientes."),
            Map.entry(ReportSection.SOCIAL_DIAGNOSIS, "Problemas relativos al grupo primario de apoyo (distanciamiento de su hija) y problemas laborales (desempleo)."),
            Map.entry(ReportSection.SOCIAL_OBJECTIVES, "Inscribirse en la bolsa de empleo sociosanitario. Ordenar sus gastos. Recuperar el régimen de visitas con su hija."),
            Map.entry(ReportSection.HEALTH_AT_ENTRY, "Buen estado general. Anemia ferropénica leve e insomnio de conciliación. Peso 56 kg."),
            Map.entry(ReportSection.DETOX_AT_ENTRY, "Desintoxicación hospitalaria de alcohol completada, sin síntomas de abstinencia al ingreso. Control de orina al ingreso negativo."),
            Map.entry(ReportSection.HEALTH_DIAGNOSIS_MID_STAY, "Anemia ferropénica en tratamiento. Insomnio en remisión."),
            Map.entry(ReportSection.HEALTH_EVOLUTION, "Buena evolución. Tratamiento con hierro oral bien tolerado. Duerme siete horas sin medicación desde el segundo mes."),
            Map.entry(ReportSection.TOXICOLOGICAL_CONTROLS, "Diez controles de orina aleatorios desde el ingreso, todos negativos."),
            Map.entry(ReportSection.HEALTH_OBJECTIVES, "Completar el tratamiento de la anemia y repetir la analítica en dos meses."),
            Map.entry(ReportSection.PSYCHO_ENTRY_ASSESSMENT, "Ingresa con conciencia del problema y demanda propia de tratamiento. Ánimo bajo y sentimientos de culpa por la relación con su hija."),
            Map.entry(ReportSection.MOTIVATION_MID_STAY, "Motivación interna y estable. Se sitúa en la fase de acción y participa activamente en los grupos."),
            Map.entry(ReportSection.PSYCHO_EVALUATION_MID_STAY, "Mejoría del estado de ánimo (BDI-II de 22 al ingreso a 10). Identifica bien las situaciones de riesgo, ligadas a la soledad."),
            Map.entry(ReportSection.PSYCHO_DIAGNOSIS, "Trastorno por consumo de alcohol, moderado, en remisión inicial en entorno controlado."),
            Map.entry(ReportSection.PSYCHO_INTERVENTION, "Sesiones individuales quincenales centradas en la culpa, la autoestima y la prevención de recaídas."),
            Map.entry(ReportSection.PSYCHO_OBJECTIVES, "Consolidar las estrategias de afrontamiento y preparar las salidas terapéuticas de fin de semana."),
            Map.entry(ReportSection.ADAPTATION_HABITS, "Muy buenos hábitos de higiene y de orden desde el ingreso."),
            Map.entry(ReportSection.ADAPTATION_RESPONSIBILITIES, "Responsable de la enfermería del centro: lo hace con autonomía y cuidado."),
            Map.entry(ReportSection.ADAPTATION_RULES, "Cumple la normativa sin ninguna incidencia."),
            Map.entry(ReportSection.ADAPTATION_WORKSHOPS, "Asiste al taller ocupacional y al de informática con regularidad."),
            Map.entry(ReportSection.ADAPTATION_LEISURE, "Retoma la lectura y participa en las salidas deportivas."),
            Map.entry(ReportSection.PEER_RELATIONSHIP, "Buena integración. Tiende a cuidar de los demás; se trabaja no descuidarse a sí misma."),
            Map.entry(ReportSection.TEAM_RELATIONSHIP, "Relación de confianza con el equipo."),
            Map.entry(ReportSection.EDUCATIONAL_OBJECTIVES, "Organizar su ocio de cara a las salidas y delegar parte de sus responsabilidades."),
            Map.entry(ReportSection.REFERENCE_RELATIVES, "Su madre, con visita semanal, y su hija, con contacto telefónico desde el segundo mes."),
            Map.entry(ReportSection.FAMILY_RELATIONSHIP, "Relación muy cercana con la madre. Con su hija el contacto mejora: se prepara una primera visita en el centro."),
            Map.entry(ReportSection.PROGRAM_DURATION_FORECAST, "Ocho meses en total, con salidas progresivas a partir del cuarto.")));

    // Informe final de un antiguo residente (Óscar Lozano Prieto), alta terapéutica
    static final Map<ReportSection, String> FINAL = new EnumMap<>(Map.ofEntries(
            Map.entry(ReportSection.SOCIAL_FAMILY_SITUATION, "Tiene 46 años, está casado y tiene dos hijos. Ha convivido con su familia durante las salidas y vuelve al domicilio familiar en Gandia."),
            Map.entry(ReportSection.WORK_EDUCATION_SITUATION, "Formación profesional de mecánica. Durante el programa ha conseguido un contrato en un taller de su localidad, que empieza al mes del alta."),
            Map.entry(ReportSection.JUDICIAL_SITUATION, "Sin causas pendientes."),
            Map.entry(ReportSection.SOCIAL_EVOLUTION, "Ha regularizado sus deudas con un plan de pagos y ha encontrado empleo. Objetivos sociales alcanzados."),
            Map.entry(ReportSection.SOCIAL_DIAGNOSIS, "Sin problemas psicosociales relevantes al alta."),
            Map.entry(ReportSection.TOXICOLOGICAL_HISTORY, "Consumo de cocaína desde los 25 años y de alcohol asociado. Dos tratamientos ambulatorios previos."),
            Map.entry(ReportSection.HEALTH_AT_ENTRY, "Buen estado general. Hipertensión arterial leve."),
            Map.entry(ReportSection.HEALTH_EVOLUTION_DURING_PROGRAM, "Tensión arterial controlada con dieta y ejercicio, sin necesidad de fármacos."),
            Map.entry(ReportSection.TOXICOLOGICAL_CONTROLS_DURING_PROGRAM, "Veinticuatro controles de orina aleatorios durante la estancia, todos negativos."),
            Map.entry(ReportSection.OTHER_MEDICAL_CARE, "Revisión odontológica y una consulta de traumatología por un esguince de tobillo."),
            Map.entry(ReportSection.HEALTH_DIAGNOSIS_FINAL, "Hipertensión arterial controlada."),
            Map.entry(ReportSection.HEALTH_AT_DISCHARGE, "Sale sin tratamiento farmacológico. Control de la tensión en su centro de salud cada tres meses."),
            Map.entry(ReportSection.PSYCHO_INITIAL_ASSESSMENT, "Ingresó con conciencia parcial del problema y motivación externa (presión familiar)."),
            Map.entry(ReportSection.PSYCHO_EVALUATION_FINAL, "Ha desarrollado estrategias eficaces ante el deseo de consumir y mejor tolerancia a la frustración."),
            Map.entry(ReportSection.PSYCHO_DIAGNOSIS, "Trastorno por consumo de cocaína, grave, en remisión mantenida en entorno controlado."),
            Map.entry(ReportSection.PSYCHO_INTERVENTION_EVOLUTION, "Veinte sesiones individuales. Objetivos de prevención de recaídas y comunicación familiar alcanzados."),
            Map.entry(ReportSection.MOTIVATION_FINAL, "Motivación interna consolidada y compromiso con el seguimiento."),
            Map.entry(ReportSection.PSYCHO_OUTINGS, "Ocho salidas terapéuticas, todas sin consumo, con una valoración positiva de la familia."),
            Map.entry(ReportSection.PSYCHO_AT_DISCHARGE, "Sale estable, con un plan de prevención de recaídas escrito y una red de apoyo sólida."),
            Map.entry(ReportSection.ADAPTATION_HABITS, "Hábitos de autocuidado y de orden consolidados."),
            Map.entry(ReportSection.ADAPTATION_RESPONSIBILITIES, "Responsable del huerto durante cuatro meses."),
            Map.entry(ReportSection.ADAPTATION_RULES, "Cumplió la normativa del centro durante toda la estancia."),
            Map.entry(ReportSection.ADAPTATION_WORKSHOPS, "Taller de mantenimiento, en el que enseñó a los compañeros."),
            Map.entry(ReportSection.ADAPTATION_LEISURE, "Organizó el torneo de fútbol del centro y retomó el ciclismo."),
            Map.entry(ReportSection.PEER_RELATIONSHIP, "Referente positivo para el grupo."),
            Map.entry(ReportSection.TEAM_RELATIONSHIP, "Relación de confianza y colaboración con el equipo."),
            Map.entry(ReportSection.EDUCATIONAL_OUTINGS, "Salidas bien planificadas y cumplidas en todos los casos."),
            Map.entry(ReportSection.REFERENCE_RELATIVES, "Su mujer y sus dos hijos, que han participado en las sesiones familiares."),
            Map.entry(ReportSection.FAMILY_RELATIONSHIP, "La comunicación familiar ha mejorado de forma notable durante el programa."),
            Map.entry(ReportSection.FAMILY_AT_DISCHARGE, "Vuelve al domicilio familiar con el apoyo de su mujer y de sus hijos."),
            Map.entry(ReportSection.DISCHARGE_GROUNDS, "Alta terapéutica: ha alcanzado los objetivos del programa."),
            Map.entry(ReportSection.AFTER_DISCHARGE_OBJECTIVES, "Seguimiento mensual en la UCA, grupo de apoyo semanal y mantener el empleo y la abstinencia.")));

    // Lo que se anota en el diario, por turnos (se reparten a lo largo de los días)
    static final List<String> MORNING_NOTES = List.of(
            "Mañana tranquila. Taller ocupacional sin incidencias.",
            "Salida al centro de salud para las analíticas de dos residentes. Todo en orden.",
            "Asamblea a las 9:30: se reparten las responsabilidades de la semana.",
            "Buena participación en el taller. Un residente llega tarde al desayuno y se habla con él.",
            "Visita de la familia de un residente a mediodía, sin incidencias.");
    static final List<String> AFTERNOON_NOTES = List.of(
            "Grupo de prevención de recaídas a las 17:00 con buena participación.",
            "Tarde de deporte en el polideportivo. Vuelta a las 19:30.",
            "Llamadas a las familias según el horario. Sin incidencias.",
            "Videoforum: se comenta la película en grupo.",
            "Paseo hasta el río y merienda en el centro.");
    static final List<String> NIGHT_NOTES = List.of(
            "Noche sin incidencias. Todos descansan a las 23:30.",
            "Un residente con dificultades para dormir; se habla con él y se tranquiliza.",
            "Noche tranquila.");

    static final List<String> NOTICES = List.of(
            "Esta semana llegan dos derivaciones nuevas de la UCA. Preparad las habitaciones 3 y 5.",
            "El grupo terapéutico del viernes pasa a las 17:00.",
            "Revisión de analíticas el jueves por la mañana: los residentes en ayunas.",
            "Salida terapéutica al río el sábado a las 10:30, vuelta prevista a las 13:30.",
            "Recordad cerrar los informes de media estancia que ya están completos.",
            "La furgoneta pasa la ITV el martes: las salidas de ese día, en transporte público.");
}
