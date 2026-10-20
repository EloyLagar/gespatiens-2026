package com.viller.gespatiens.access.users.infrastructure.config;

import com.viller.gespatiens.access.users.application.ChangeOwnPasswordUseCase;
import com.viller.gespatiens.access.users.application.GetCurrentUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.application.UpdateOwnProfileUseCase;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí,
// así la capa de aplicación no depende del framework
@Configuration
public class UserBeanConfiguration {

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new RegisterUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository) {
        return new ListUsersUseCase(userRepository);
    }

    @Bean
    public GetUserUseCase getUserUseCase(UserRepository userRepository) {
        return new GetUserUseCase(userRepository);
    }

    @Bean
    public GetCurrentUserUseCase getCurrentUserUseCase(UserRepository userRepository) {
        return new GetCurrentUserUseCase(userRepository);
    }

    @Bean
    public UpdateOwnProfileUseCase updateOwnProfileUseCase(UserRepository userRepository) {
        return new UpdateOwnProfileUseCase(userRepository);
    }

    @Bean
    public ChangeOwnPasswordUseCase changeOwnPasswordUseCase(UserRepository userRepository,
                                                             PasswordHasher passwordHasher) {
        return new ChangeOwnPasswordUseCase(userRepository, passwordHasher);
    }
}
