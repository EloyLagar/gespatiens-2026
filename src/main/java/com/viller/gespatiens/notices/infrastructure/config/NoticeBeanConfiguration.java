package com.viller.gespatiens.notices.infrastructure.config;

import com.viller.gespatiens.notices.application.DeleteNoticeUseCase;
import com.viller.gespatiens.notices.application.EditNoticeUseCase;
import com.viller.gespatiens.notices.application.ListNoticesUseCase;
import com.viller.gespatiens.notices.application.PublishNoticeUseCase;
import com.viller.gespatiens.notices.domain.AuthorDirectory;
import com.viller.gespatiens.notices.domain.NoticeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí
@Configuration
public class NoticeBeanConfiguration {

    // El reloj se inyecta para que los tests puedan fijar la hora de publicación
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ListNoticesUseCase listNoticesUseCase(NoticeRepository noticeRepository, AuthorDirectory authorDirectory) {
        return new ListNoticesUseCase(noticeRepository, authorDirectory);
    }

    @Bean
    public PublishNoticeUseCase publishNoticeUseCase(NoticeRepository noticeRepository,
                                                     AuthorDirectory authorDirectory, Clock clock) {
        return new PublishNoticeUseCase(noticeRepository, authorDirectory, clock);
    }

    @Bean
    public EditNoticeUseCase editNoticeUseCase(NoticeRepository noticeRepository,
                                               AuthorDirectory authorDirectory, Clock clock) {
        return new EditNoticeUseCase(noticeRepository, authorDirectory, clock);
    }

    @Bean
    public DeleteNoticeUseCase deleteNoticeUseCase(NoticeRepository noticeRepository, AuthorDirectory authorDirectory) {
        return new DeleteNoticeUseCase(noticeRepository, authorDirectory);
    }
}
