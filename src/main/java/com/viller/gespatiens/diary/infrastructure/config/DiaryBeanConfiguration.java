package com.viller.gespatiens.diary.infrastructure.config;

import com.viller.gespatiens.diary.application.DeleteActivityUseCase;
import com.viller.gespatiens.diary.application.DeleteOutingUseCase;
import com.viller.gespatiens.diary.application.DeleteReductionUseCase;
import com.viller.gespatiens.diary.application.GetDiaryDayUseCase;
import com.viller.gespatiens.diary.application.RecordAttendanceUseCase;
import com.viller.gespatiens.diary.application.RecordEvaluationsUseCase;
import com.viller.gespatiens.diary.application.RecordOutingUseCase;
import com.viller.gespatiens.diary.application.RecordReductionUseCase;
import com.viller.gespatiens.diary.application.RecordShiftUseCase;
import com.viller.gespatiens.diary.application.RescheduleActivityUseCase;
import com.viller.gespatiens.diary.application.ScheduleActivityUseCase;
import com.viller.gespatiens.diary.application.UpdateOutingUseCase;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;
import com.viller.gespatiens.diary.domain.TherapeuticOutingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí. El Clock lo define NoticeBeanConfiguration
@Configuration
public class DiaryBeanConfiguration {

    @Bean
    public GetDiaryDayUseCase getDiaryDayUseCase(ShiftLogRepository shiftLogRepository,
                                                 ReductionRepository reductionRepository,
                                                 ActivityRepository activityRepository,
                                                 TherapeuticOutingRepository outingRepository,
                                                 StaffDirectory staffDirectory,
                                                 ResidentDirectory residentDirectory) {
        return new GetDiaryDayUseCase(shiftLogRepository, reductionRepository, activityRepository, outingRepository,
                staffDirectory, residentDirectory);
    }

    @Bean
    public RecordShiftUseCase recordShiftUseCase(ShiftLogRepository shiftLogRepository,
                                                 StaffDirectory staffDirectory, Clock clock) {
        return new RecordShiftUseCase(shiftLogRepository, staffDirectory, clock);
    }

    @Bean
    public RecordReductionUseCase recordReductionUseCase(ReductionRepository reductionRepository,
                                                         ResidentDirectory residentDirectory, Clock clock) {
        return new RecordReductionUseCase(reductionRepository, residentDirectory, clock);
    }

    @Bean
    public DeleteReductionUseCase deleteReductionUseCase(ReductionRepository reductionRepository) {
        return new DeleteReductionUseCase(reductionRepository);
    }

    @Bean
    public ScheduleActivityUseCase scheduleActivityUseCase(ActivityRepository activityRepository,
                                                           ReductionRepository reductionRepository,
                                                           ResidentDirectory residentDirectory) {
        return new ScheduleActivityUseCase(activityRepository, reductionRepository, residentDirectory);
    }

    @Bean
    public RescheduleActivityUseCase rescheduleActivityUseCase(ActivityRepository activityRepository,
                                                               ReductionRepository reductionRepository,
                                                               ResidentDirectory residentDirectory) {
        return new RescheduleActivityUseCase(activityRepository, reductionRepository, residentDirectory);
    }

    @Bean
    public DeleteActivityUseCase deleteActivityUseCase(ActivityRepository activityRepository) {
        return new DeleteActivityUseCase(activityRepository);
    }

    @Bean
    public RecordAttendanceUseCase recordAttendanceUseCase(ActivityRepository activityRepository,
                                                           ReductionRepository reductionRepository,
                                                           ResidentDirectory residentDirectory, Clock clock) {
        return new RecordAttendanceUseCase(activityRepository, reductionRepository, residentDirectory, clock);
    }

    @Bean
    public RecordEvaluationsUseCase recordEvaluationsUseCase(ActivityRepository activityRepository,
                                                             ReductionRepository reductionRepository,
                                                             ResidentDirectory residentDirectory) {
        return new RecordEvaluationsUseCase(activityRepository, reductionRepository, residentDirectory);
    }

    @Bean
    public RecordOutingUseCase recordOutingUseCase(TherapeuticOutingRepository outingRepository,
                                                   ResidentDirectory residentDirectory) {
        return new RecordOutingUseCase(outingRepository, residentDirectory);
    }

    @Bean
    public UpdateOutingUseCase updateOutingUseCase(TherapeuticOutingRepository outingRepository,
                                                   ResidentDirectory residentDirectory) {
        return new UpdateOutingUseCase(outingRepository, residentDirectory);
    }

    @Bean
    public DeleteOutingUseCase deleteOutingUseCase(TherapeuticOutingRepository outingRepository) {
        return new DeleteOutingUseCase(outingRepository);
    }
}
