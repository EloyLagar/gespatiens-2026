package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportHeader;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.SectionEntry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReportRepositoryJpaAdapter implements ReportRepository {

    private final ReportJpaRepository reportJpaRepository;

    public ReportRepositoryJpaAdapter(ReportJpaRepository reportJpaRepository) {
        this.reportJpaRepository = reportJpaRepository;
    }

    @Override
    public Report save(Report report) {
        // saveAndFlush: si otra petición guardó el informe a la vez, el conflicto de versión salta aquí y no
        // al terminar la petición, cuando ya no se puede traducir a una respuesta clara
        return toDomain(reportJpaRepository.saveAndFlush(toEntity(report)));
    }

    @Override
    public void addIfMissing(Report report) {
        try {
            reportJpaRepository.saveAndFlush(toEntity(report));
        } catch (DataIntegrityViolationException alreadyThere) {
            // ux_reports_one_per_stay: otra petición ha creado el informe de esa estancia a la vez
        }
    }

    @Override
    public Optional<Report> findById(UUID id) {
        return reportJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Report> findByPatient(UUID patientId) {
        return reportJpaRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Report> findAll() {
        return reportJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private ReportJpaEntity toEntity(Report report) {
        ReportJpaEntity entity = new ReportJpaEntity();
        entity.setId(report.getId());
        entity.setPatientId(report.getPatientId());
        entity.setType(report.getType());
        entity.setStayStart(report.getStayStart());
        entity.setState(report.getState());
        ReportHeader header = report.getHeader();
        entity.setRequestNumber(header.requestNumber());
        entity.setReferenceCenter(header.referenceCenter());
        entity.setNip(header.nip());
        entity.setEntryDate(header.entryDate());
        entity.setExitDate(header.exitDate());
        entity.setExitReason(header.exitReason());
        entity.setDueDate(header.dueDate());
        entity.setHeaderVersion(report.getHeaderVersion());
        entity.setCreatedBy(report.getCreatedBy().orElse(null));
        entity.setCreatedAt(report.getCreatedAt());
        entity.setClosedBy(report.getClosedBy().orElse(null));
        entity.setClosedAt(report.getClosedAt().orElse(null));
        entity.setRevision(report.getRevision().orElse(null));
        Map<String, SectionEmbeddable> sections = new HashMap<>();
        report.getSections().forEach((section, entry) -> sections.put(section.name(),
                new SectionEmbeddable(entry.text(), entry.authorId(), entry.updatedAt(), entry.version())));
        entity.setSections(sections);
        return entity;
    }

    private Report toDomain(ReportJpaEntity entity) {
        Map<ReportSection, SectionEntry> sections = new EnumMap<>(ReportSection.class);
        entity.getSections().forEach((section, row) -> sections.put(ReportSection.valueOf(section),
                new SectionEntry(row.getText(), row.getAuthorId(), row.getUpdatedAt(), row.getVersion())));
        return Report.reconstitute(
                entity.getId(),
                entity.getPatientId(),
                entity.getType(),
                entity.getStayStart(),
                entity.getState(),
                new ReportHeader(entity.getRequestNumber(), entity.getReferenceCenter(), entity.getNip(),
                        entity.getEntryDate(), entity.getExitDate(), entity.getExitReason(), entity.getDueDate()),
                entity.getHeaderVersion(),
                sections,
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getClosedBy(),
                entity.getClosedAt(),
                entity.getRevision());
    }
}
