package com.viller.gespatiens.reports.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Un informe de media estancia o de fin de programa de una estancia de un residente (RF1). Cada residente
// tiene uno de cada tipo por estancia: se crean solos al entrar y los de estancias anteriores se conservan
// como historial. Cada área la redacta su especialidad y cada apartado se guarda por separado, para que
// varios profesionales escriban a la vez sin pisarse (RF8). Quién escribió cada apartado es el Writes_about
// de la memoria
public class Report {

    private final UUID id;
    private final UUID patientId;
    private final ReportType type;
    // El día en que empezó la estancia: con el residente y el tipo, identifica el informe
    private final LocalDate stayStart;
    private ReportState state;
    private ReportHeader header;
    private int headerVersion;
    private final Map<ReportSection, SectionEntry> sections;
    // null: lo creó la aplicación al empezar la estancia (o quien lo creó ya no está en la plantilla)
    private final UUID createdBy;
    private final Instant createdAt;
    private UUID closedBy;
    private Instant closedAt;
    // Lo lleva la persistencia (bloqueo optimista). null: todavía no se ha guardado nunca
    private final Long revision;

    private Report(UUID id, UUID patientId, ReportType type, LocalDate stayStart, ReportState state,
                   ReportHeader header, int headerVersion, Map<ReportSection, SectionEntry> sections,
                   UUID createdBy, Instant createdAt, UUID closedBy, Instant closedAt, Long revision) {
        if (id == null || patientId == null || type == null || state == null || stayStart == null) {
            throw new IllegalArgumentException("El informe necesita id, residente, tipo, estancia y estado");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Falta la fecha en que se creó el informe");
        }
        if ((state == ReportState.CLOSED) != (closedAt != null)) {
            throw new IllegalArgumentException("Solo un informe cerrado tiene fecha de cierre");
        }
        if (headerVersion < 0) {
            throw new IllegalArgumentException("La versión de la cabecera no puede ser negativa");
        }
        Map<ReportSection, SectionEntry> copy = new EnumMap<>(ReportSection.class);
        if (sections != null) {
            sections.forEach((section, entry) -> {
                if (!section.belongsTo(type)) {
                    throw new IllegalArgumentException("El apartado " + section + " no es de un " + type.title());
                }
                copy.put(section, entry);
            });
        }
        this.id = id;
        this.patientId = patientId;
        this.type = type;
        this.stayStart = stayStart;
        this.state = state;
        this.header = (header == null ? ReportHeader.forStay(stayStart) : header).forType(type);
        this.headerVersion = headerVersion;
        this.sections = copy;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        this.revision = revision;
    }

    // El informe vacío de una estancia: en elaboración, con la cabecera que trae la ficha
    public static Report forStay(UUID patientId, ReportType type, LocalDate stayStart, Instant now) {
        return new Report(UUID.randomUUID(), patientId, type, stayStart, ReportState.DRAFT,
                ReportHeader.forStay(stayStart), 0, Map.of(), null, now, null, null, null);
    }

    public static Report reconstitute(UUID id, UUID patientId, ReportType type, LocalDate stayStart,
                                      ReportState state, ReportHeader header, int headerVersion,
                                      Map<ReportSection, SectionEntry> sections, UUID createdBy, Instant createdAt,
                                      UUID closedBy, Instant closedAt, Long revision) {
        return new Report(id, patientId, type, stayStart, state, header, headerVersion, sections, createdBy,
                createdAt, closedBy, closedAt, revision);
    }

    // Redactar un apartado. expectedVersion es la versión que el empleado tenía delante (0 si estaba vacío):
    // si no coincide con la actual, otra persona lo ha cambiado mientras tanto y no se guarda (RF8).
    // tutor: quien escribe es tutor del residente. Un texto en blanco vacía el apartado
    public void write(ReportSection section, String text, int expectedVersion, StaffMember writer, boolean tutor,
                      Instant now) {
        if (section == null) {
            throw new IllegalArgumentException("Indica el apartado del informe");
        }
        if (!section.belongsTo(type)) {
            throw new IllegalArgumentException("Ese apartado no forma parte de un " + type.title().toLowerCase());
        }
        ensureDraft();
        if (!section.area().canBeWrittenBy(writer, tutor)) {
            throw new AreaNotWritableException(section.area(), type);
        }
        SectionEntry current = sections.get(section);
        int currentVersion = current == null ? 0 : current.version();
        if (expectedVersion != currentVersion) {
            throw new SectionConflictException(section, current);
        }
        String trimmed = text == null ? "" : text.strip();
        if (current != null && trimmed.equals(current.text())) {
            return; // Guardar sin cambios no cambia el autor ni la versión
        }
        if (trimmed.isEmpty()) {
            sections.remove(section);
        } else {
            sections.put(section, new SectionEntry(trimmed, writer.id(), now, currentVersion + 1));
        }
    }

    // Lo que se escribe a mano en la cabecera. La completa cualquier empleado: son datos administrativos.
    // dueDate: cuándo toca el de media estancia (null: la de por defecto)
    public void editHeader(String requestNumber, String referenceCenter, String nip, String exitReason,
                           LocalDate dueDate, int expectedVersion) {
        ensureDraft();
        if (expectedVersion != headerVersion) {
            throw new HeaderConflictException();
        }
        ReportHeader edited = header.withEditable(requestNumber, referenceCenter, nip, exitReason, dueDate)
                .forType(type);
        if (edited.equals(header)) {
            return;
        }
        this.header = edited;
        this.headerVersion++;
    }

    // La estancia ha terminado: la fecha de salida de la ficha pasa a la cabecera. Lo hace la aplicación al dar
    // de baja, así que también en un informe cerrado (es un dato de la ficha, no algo redactado)
    public boolean recordStayEnd(LocalDate exitDate) {
        if (exitDate == null || exitDate.equals(header.exitDate())) {
            return false;
        }
        this.header = header.withExitDate(exitDate);
        return true;
    }

    // "Fecha y firma de la Dirección del recurso": la administración da el informe por terminado y ya no
    // se puede modificar por error
    public void close(StaffMember by, Instant now) {
        ensureAdministrator(by);
        ensureDraft();
        this.state = ReportState.CLOSED;
        this.closedBy = by.id();
        this.closedAt = now;
    }

    // Para corregir un informe entregado
    public void reopen(StaffMember by) {
        ensureAdministrator(by);
        if (state != ReportState.CLOSED) {
            throw new IllegalArgumentException("El informe ya está en elaboración");
        }
        this.state = ReportState.DRAFT;
        this.closedBy = null;
        this.closedAt = null;
    }

    public boolean isDraft() {
        return state == ReportState.DRAFT;
    }

    // Quién ha escrito en el informe, en el orden de los apartados: "El presente informe ha sido elaborado
    // por los siguientes profesionales"
    public Set<UUID> authorIds() {
        Set<UUID> authors = new LinkedHashSet<>();
        sections.values().forEach(entry -> entry.author().ifPresent(authors::add));
        return authors;
    }

    // Cuándo se tocó por última vez: el último apartado guardado o, si no hay ninguno, cuando se creó
    public Instant lastUpdate() {
        return sections.values().stream().map(SectionEntry::updatedAt).max(Instant::compareTo).orElse(createdAt);
    }

    public int writtenSections() {
        return sections.size();
    }

    public int totalSections() {
        return ReportSection.of(type).size();
    }

    private void ensureDraft() {
        if (state != ReportState.DRAFT) {
            throw new ReportClosedException();
        }
    }

    private static void ensureAdministrator(StaffMember member) {
        if (member == null || !member.administrator()) {
            throw new ReportNotManageableException();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public ReportType getType() {
        return type;
    }

    public LocalDate getStayStart() {
        return stayStart;
    }

    public ReportState getState() {
        return state;
    }

    public ReportHeader getHeader() {
        return header;
    }

    public int getHeaderVersion() {
        return headerVersion;
    }

    // En el orden de los apartados del informe
    public Map<ReportSection, SectionEntry> getSections() {
        return Collections.unmodifiableMap(sections);
    }

    public Optional<SectionEntry> getSection(ReportSection section) {
        return Optional.ofNullable(sections.get(section));
    }

    public Optional<UUID> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<UUID> getClosedBy() {
        return Optional.ofNullable(closedBy);
    }

    public Optional<Instant> getClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    public Optional<Long> getRevision() {
        return Optional.ofNullable(revision);
    }
}
