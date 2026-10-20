package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.application.DiaryDayView.OutingView;
import com.viller.gespatiens.diary.application.DiaryDayView.ReductionView;
import com.viller.gespatiens.diary.application.DiaryDayView.ShiftView;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;
import com.viller.gespatiens.diary.domain.StaffMember;
import com.viller.gespatiens.diary.domain.TherapeuticOutingRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// La memoria: "se podrá seleccionar la fecha de la página del diario a la que se desea acceder"
public class GetDiaryDayUseCase {

    private final ShiftLogRepository shiftLogRepository;
    private final ReductionRepository reductionRepository;
    private final ActivityRepository activityRepository;
    private final TherapeuticOutingRepository outingRepository;
    private final StaffDirectory staffDirectory;
    private final ResidentDirectory residentDirectory;

    public GetDiaryDayUseCase(ShiftLogRepository shiftLogRepository, ReductionRepository reductionRepository,
                              ActivityRepository activityRepository, TherapeuticOutingRepository outingRepository,
                              StaffDirectory staffDirectory, ResidentDirectory residentDirectory) {
        this.shiftLogRepository = shiftLogRepository;
        this.reductionRepository = reductionRepository;
        this.activityRepository = activityRepository;
        this.outingRepository = outingRepository;
        this.staffDirectory = staffDirectory;
        this.residentDirectory = residentDirectory;
    }

    public DiaryDayView execute(LocalDate date) {
        Map<DayPart, ShiftLog> logs = shiftLogRepository.findByDate(date).stream()
                .collect(Collectors.toMap(ShiftLog::getPart, Function.identity()));
        // Siempre los tres turnos, en orden; los que nadie ha rellenado salen vacíos
        List<ShiftView> shifts = Arrays.stream(DayPart.values())
                .map(part -> Optional.ofNullable(logs.get(part))
                        .map(log -> shiftView(log, staffDirectory))
                        .orElseGet(() -> new ShiftView(part, List.of(), Optional.empty(), Optional.empty(), Optional.empty())))
                .toList();

        List<Reduction> dayReductions = reductionRepository.findByDate(date);
        List<ReductionView> reductions = dayReductions.stream()
                .map(r -> reductionView(r, residentDirectory))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ReductionView::resident, DiaryViews.BY_PLACE))
                .toList();

        Map<UUID, Reduction> reductionsByPatient = dayReductions.stream()
                .collect(Collectors.toMap(Reduction::getPatientId, Function.identity()));
        List<ActivityView> activities = activityRepository.findByDate(date).stream()
                .map(a -> DiaryViews.activityView(a, residentDirectory, reductionsByPatient))
                .toList();

        List<OutingView> outings = outingRepository.findByDate(date).stream()
                .map(o -> DiaryViews.outingView(o, residentDirectory))
                .flatMap(Optional::stream)
                .toList();

        return new DiaryDayView(date, shifts, reductions, activities, outings);
    }

    static ShiftView shiftView(ShiftLog log, StaffDirectory staffDirectory) {
        List<StaffMember> staff = log.getStaffIds().stream()
                .map(staffDirectory::find)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(StaffMember::name))
                .toList();
        return new ShiftView(log.getPart(), staff, log.getNotes(),
                log.getUpdatedBy().flatMap(staffDirectory::find), Optional.of(log.getUpdatedAt()));
    }

    static Optional<ReductionView> reductionView(Reduction reduction, ResidentDirectory residentDirectory) {
        return residentDirectory.find(reduction.getPatientId())
                .map(r -> new ReductionView(r, reduction.getPartial(), reduction.isTotal(), reduction.isSport()));
    }
}
