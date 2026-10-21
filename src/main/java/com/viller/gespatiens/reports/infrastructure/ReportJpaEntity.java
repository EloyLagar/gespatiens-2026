package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.reports.domain.ReportState;
import com.viller.gespatiens.reports.domain.ReportType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class ReportJpaEntity {

    // El id lo genera el dominio (UUID), no la base de datos: sin @GeneratedValue
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType type;

    @Column(nullable = false)
    private LocalDate stayStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportState state;

    @Column(length = 30)
    private String requestNumber;

    @Column(length = 150)
    private String referenceCenter;

    @Column(length = 30)
    private String nip;

    private LocalDate entryDate;

    private LocalDate exitDate;

    @Column(length = 200)
    private String exitReason;

    @Column(nullable = false)
    private int headerVersion;

    // Solo en el de media estancia: cuándo toca
    private LocalDate dueDate;

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    private UUID createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    private UUID closedBy;

    private Instant closedAt;

    // Bloqueo optimista: null mientras el informe no se ha guardado nunca, así Spring Data sabe que es nuevo
    @Version
    private Long revision;

    // apartado -> lo escrito. EAGER: el adaptador traduce a dominio fuera de la transacción
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_sections", joinColumns = @JoinColumn(name = "report_id"))
    @MapKeyColumn(name = "section", length = 60)
    private Map<String, SectionEmbeddable> sections = new HashMap<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public LocalDate getStayStart() {
        return stayStart;
    }

    public void setStayStart(LocalDate stayStart) {
        this.stayStart = stayStart;
    }

    public ReportState getState() {
        return state;
    }

    public void setState(ReportState state) {
        this.state = state;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getReferenceCenter() {
        return referenceCenter;
    }

    public void setReferenceCenter(String referenceCenter) {
        this.referenceCenter = referenceCenter;
    }

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }

    public void setExitDate(LocalDate exitDate) {
        this.exitDate = exitDate;
    }

    public String getExitReason() {
        return exitReason;
    }

    public void setExitReason(String exitReason) {
        this.exitReason = exitReason;
    }

    public int getHeaderVersion() {
        return headerVersion;
    }

    public void setHeaderVersion(int headerVersion) {
        this.headerVersion = headerVersion;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    public Map<String, SectionEmbeddable> getSections() {
        return sections;
    }

    public void setSections(Map<String, SectionEmbeddable> sections) {
        this.sections = sections;
    }
}
