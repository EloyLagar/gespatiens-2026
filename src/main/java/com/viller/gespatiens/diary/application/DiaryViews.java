package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.application.DiaryDayView.AttendanceRow;
import com.viller.gespatiens.diary.application.DiaryDayView.OutingView;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.TherapeuticOuting;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Arma las vistas de actividades y salidas. Lo comparten la página del día y los casos de uso que
// devuelven una actividad o una salida recién guardada
final class DiaryViews {

    static final Comparator<Resident> BY_PLACE = Comparator
            .comparing(Resident::number, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Resident::surname);

    private DiaryViews() {
    }

    static Map<UUID, Reduction> reductionsByPatient(ReductionRepository reductions, Activity activity) {
        return reductions.findByDate(activity.getDate()).stream()
                .collect(Collectors.toMap(Reduction::getPatientId, Function.identity()));
    }

    // La lista de una actividad: quienes están hoy en el centro, más quienes ya tenían asistencia anotada
    // aunque después hayan salido (para que una lista pasada no pierda a nadie)
    static ActivityView activityView(Activity activity, ResidentDirectory residents, Map<UUID, Reduction> reductions) {
        Map<UUID, Resident> people = new LinkedHashMap<>();
        residents.findActive().forEach(r -> people.put(r.id(), r));
        activity.getAttendance().keySet().stream()
                .filter(id -> !people.containsKey(id))
                .map(residents::find)
                .flatMap(Optional::stream)
                .forEach(r -> people.put(r.id(), r));

        List<AttendanceRow> rows = people.values().stream()
                .sorted(BY_PLACE)
                .map(resident -> {
                    Optional<Reduction> reduction = Optional.ofNullable(reductions.get(resident.id()));
                    boolean reduced = reduction.map(r -> r.excuses(activity.getType())).orElse(false);
                    return new AttendanceRow(
                            resident,
                            reduced ? Optional.empty() : Optional.ofNullable(activity.getAttendance().get(resident.id())),
                            reduced,
                            reduction.flatMap(Reduction::getPartial));
                })
                .toList();
        return new ActivityView(activity.getId(), activity.getStartTime(), activity.getType(), activity.getTitle(), rows);
    }

    static Optional<OutingView> outingView(TherapeuticOuting outing, ResidentDirectory residents) {
        return residents.find(outing.getPatientId())
                .map(r -> new OutingView(outing.getId(), r, outing.getLeftAt(), outing.getReturnedAt(), outing.getNotes()));
    }
}
