package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ActivityRepositoryJpaAdapter implements ActivityRepository {

    private final ActivityJpaRepository activityJpaRepository;

    public ActivityRepositoryJpaAdapter(ActivityJpaRepository activityJpaRepository) {
        this.activityJpaRepository = activityJpaRepository;
    }

    @Override
    public Activity save(Activity activity) {
        return toDomain(activityJpaRepository.save(toEntity(activity)));
    }

    @Override
    public Optional<Activity> findById(UUID id) {
        return activityJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Activity> findByDate(LocalDate date) {
        return activityJpaRepository.findByDateOrderByStartTime(date).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Activity activity) {
        activityJpaRepository.deleteById(activity.getId());
    }

    private ActivityJpaEntity toEntity(Activity activity) {
        ActivityJpaEntity entity = new ActivityJpaEntity();
        entity.setId(activity.getId());
        entity.setDate(activity.getDate());
        entity.setStartTime(activity.getStartTime());
        entity.setType(activity.getType());
        entity.setTitle(activity.getTitle().orElse(null));
        entity.setAttendance(activity.getAttendance());
        return entity;
    }

    private Activity toDomain(ActivityJpaEntity entity) {
        return Activity.reconstitute(
                entity.getId(),
                entity.getDate(),
                entity.getStartTime(),
                entity.getType(),
                entity.getTitle(),
                entity.getAttendance());
    }
}
