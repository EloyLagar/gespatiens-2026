package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserRepositoryJpaAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = userJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(this::toDomain);
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setSpeciality(user.getSpeciality());
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getSpeciality());
    }
}