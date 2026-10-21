package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.ActivityRef;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.Cell;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.DayColumn;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView.ResidentRow;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.ResidentDirectory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// RF6, segunda forma de verlo (memoria final): "se solicita mediante un formulario un mes y un año además de
// una materia", y sale una tabla con la nota de cada residente cada día. Las notas se ponen igual que desde el
// diario: son las valoraciones de cada actividad
public class MonthlyEvaluationsUseCase {

    private final ActivityRepository activityRepository;
    private final ResidentDirectory residentDirectory;

    public MonthlyEvaluationsUseCase(ActivityRepository activityRepository, ResidentDirectory residentDirectory) {
        this.activityRepository = activityRepository;
        this.residentDirectory = residentDirectory;
    }

    public MonthlyEvaluationsView execute(YearMonth month, ActivityType type) {
        if (month == null || type == null) {
            throw new IllegalArgumentException("Indica el mes y el tipo de actividad");
        }
        Map<LocalDate, List<Activity>> byDay = activityRepository.findBetween(month.atDay(1), month.atEndOfMonth())
                .stream()
                .filter(activity -> activity.getType() == type)
                .collect(Collectors.groupingBy(Activity::getDate));

        List<DayColumn> days = month.atDay(1).datesUntil(month.atEndOfMonth().plusDays(1))
                .map(date -> new DayColumn(date, byDay.getOrDefault(date, List.of()).stream()
                        .map(a -> new ActivityRef(a.getId(), a.getStartTime(), a.getTitle(), a.version()))
                        .toList()))
                .toList();

        // Quienes están en el centro, más quienes pasaron por alguna de estas actividades aunque ya no estén
        Map<UUID, Resident> people = new LinkedHashMap<>();
        residentDirectory.findActive().forEach(r -> people.put(r.id(), r));
        byDay.values().stream().flatMap(List::stream)
                .flatMap(activity -> activity.getAttendance().keySet().stream())
                .distinct()
                .filter(id -> !people.containsKey(id))
                .map(residentDirectory::find)
                .flatMap(Optional::stream)
                .forEach(r -> people.put(r.id(), r));

        List<ResidentRow> rows = people.values().stream()
                .sorted(DiaryViews.BY_PLACE)
                .map(resident -> new ResidentRow(resident, days.stream()
                        .map(day -> cell(resident.id(), byDay.getOrDefault(day.date(), List.of())))
                        .toList()))
                .toList();
        return new MonthlyEvaluationsView(month, type, days, rows);
    }

    private static Cell cell(UUID patientId, List<Activity> activities) {
        List<Activity> attended = activities.stream().filter(a -> a.attended(patientId)).toList();
        List<BigDecimal> marks = attended.stream()
                .map(a -> a.getEvaluations().get(patientId))
                .filter(Objects::nonNull)
                .map(Evaluation::mark)
                .filter(Objects::nonNull)
                .toList();
        Optional<BigDecimal> mark = marks.isEmpty() ? Optional.empty() : Optional.of(marks.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(marks.size()), 1, RoundingMode.HALF_UP));
        // Con una sola actividad ese día la nota se cambia desde la tabla; con varias, desde cada actividad
        Optional<UUID> editable = activities.size() == 1 && !attended.isEmpty()
                ? Optional.of(activities.getFirst().getId())
                : Optional.empty();
        return new Cell(mark, !attended.isEmpty(), editable);
    }
}
