package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public void delete(User user) {
        userJpaRepository.deleteById(user.getId());
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAllByOrderByName().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userJpaRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id) {
        return userJpaRepository.existsByPhoneNumberAndIdNot(phoneNumber, id);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return userJpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public long countBySpeciality(Speciality speciality) {
        return userJpaRepository.countBySpeciality(speciality);
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setSpeciality(user.getSpeciality());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setLanguage(user.getLanguage());
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getSpeciality(),
                entity.getPasswordHash(),
                entity.getLanguage());
    }
}