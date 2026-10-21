package com.viller.gespatiens.reports.domain;

import java.util.Set;

// Las áreas de los informes, en el orden en que aparecen. La memoria final: "cada empleado solo tendrá acceso a
// la cumplimentación de los apartados que correspondan a su especialidad (...) los administradores deben poder
// ser capaces de modificar cualquier apartado". Las conclusiones las redactan los tutores del residente
// (decisión del centro)
public enum ReportArea {
    SOCIAL("ÁREA SOCIAL", "ÁREA DE INTERVENCIÓN SOCIAL", Set.of("SOCIAL_WORKER"), false),
    HEALTH("ÁREA DE INTERVENCIÓN SANITARIA", "ÁREA DE INTERVENCIÓN SANITARIA", Set.of("MEDICAL_TEAM"), false),
    PSYCHOLOGICAL("ÁREA DE INTERVENCIÓN PSICOLÓGICA", "ÁREA DE INTERVENCIÓN PSICOLÓGICA", Set.of("PSYCHOLOGIST"),
            false),
    EDUCATIONAL("ÁREA EDUCATIVA", "ÁREA EDUCATIVA", Set.of("SOCIAL_EDUCATOR"), false),
    FAMILY("ÁREA DE INTERVENCIÓN FAMILIAR", "ÁREA DE INTERVENCIÓN FAMILIAR", Set.of("SOCIAL_WORKER"), false),
    // Solo en el de media estancia
    DURATION("PREVISIÓN DE LA DURACIÓN DEL PROGRAMA (EN MESES)", null, Set.of(), true),
    // Solo en el final
    FOLLOW_UP(null, "ORIENTACIONES DE SEGUIMIENTO PARA LA UNIDAD DE REFERENCIA", Set.of(), true);

    private final String midStayTitle;
    private final String finalTitle;
    private final Set<String> writers;
    private final boolean writtenByTutors;

    ReportArea(String midStayTitle, String finalTitle, Set<String> writers, boolean writtenByTutors) {
        this.midStayTitle = midStayTitle;
        this.finalTitle = finalTitle;
        this.writers = writers;
        this.writtenByTutors = writtenByTutors;
    }

    // El encabezado cambia un poco de un informe a otro ("Área social" / "Área de intervención social")
    public String title(ReportType type) {
        return type == ReportType.MID_STAY ? midStayTitle : finalTitle;
    }

    // Las especialidades que la redactan
    public Set<String> writers() {
        return writers;
    }

    // La redactan los tutores del residente (un educador social y un psicólogo)
    public boolean writtenByTutors() {
        return writtenByTutors;
    }

    // tutor: quien escribe es tutor del residente del informe
    public boolean canBeWrittenBy(StaffMember member, boolean tutor) {
        return member.administrator() || writers.contains(member.speciality()) || (writtenByTutors && tutor);
    }
}
