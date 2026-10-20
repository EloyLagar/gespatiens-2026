package com.viller.gespatiens.diary.infrastructure.config;

import com.viller.gespatiens.diary.application.DeleteReductionUseCase;
import com.viller.gespatiens.diary.application.GetDiaryDayUseCase;
import com.viller.gespatiens.diary.application.RecordReductionUseCase;
import com.viller.gespatiens.diary.application.RecordShiftUseCase;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí. El Clock lo define NoticeBeanConfiguration
@Configuration
public class DiaryBeanConfiguration {

    @Bean
    public GetDiaryDayUseCase getDiaryDayUseCase(ShiftLogRepository shiftLogRepository,
                                                 ReductionRepository reductionRepository,
                                                 StaffDirectory staffDirectory,
                                                 ResidentDirectory residentDirectory) {
        return new GetDiaryDayUseCase(shiftLogRepository, reductionRepository, staffDirectory, residentDirectory);
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
}
