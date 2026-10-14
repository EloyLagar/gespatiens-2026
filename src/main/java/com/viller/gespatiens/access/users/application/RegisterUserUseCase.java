package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;

public class RegisterUserUseCase {

    private final UserRepository userRepository;

    public RegisterUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(String name, String email, String phoneNumber, String speciality) {
        User user = User.create(name, email, phoneNumber, speciality);
        return userRepository.save(user);
    }
}
