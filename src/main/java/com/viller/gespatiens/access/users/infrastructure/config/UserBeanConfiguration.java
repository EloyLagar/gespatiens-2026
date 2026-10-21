package com.viller.gespatiens.access.users.infrastructure.config;

import com.viller.gespatiens.access.users.application.AccessTokenIssuer;
import com.viller.gespatiens.access.users.application.ChangeOwnPasswordUseCase;
import com.viller.gespatiens.access.users.application.DeleteUserUseCase;
import com.viller.gespatiens.access.users.application.EditUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.LoginUseCase;
import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.application.ResetUserPasswordUseCase;
import com.viller.gespatiens.access.users.application.SignatureUseCase;
import com.viller.gespatiens.access.users.application.UpdateOwnProfileUseCase;
import com.viller.gespatiens.access.users.domain.AdministratorGuard;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.SignatureRepository;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí,
// así la capa de aplicación no depende del framework
@Configuration
public class UserBeanConfiguration {

    // El Clock lo define NoticeBeanConfiguration
    @Bean
    public SignatureUseCase signatureUseCase(UserRepository userRepository, SignatureRepository signatureRepository,
                                             Clock clock) {
        return new SignatureUseCase(userRepository, signatureRepository, clock);
    }

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
    public LoginUseCase loginUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                                     AccessTokenIssuer accessTokenIssuer) {
        return new LoginUseCase(userRepository, passwordHasher, accessTokenIssuer);
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

    @Bean
    public AdministratorGuard administratorGuard(UserRepository userRepository) {
        return new AdministratorGuard(userRepository);
    }

    @Bean
    public EditUserUseCase editUserUseCase(UserRepository userRepository, AdministratorGuard administratorGuard) {
        return new EditUserUseCase(userRepository, administratorGuard);
    }

    @Bean
    public ResetUserPasswordUseCase resetUserPasswordUseCase(UserRepository userRepository,
                                                             PasswordHasher passwordHasher) {
        return new ResetUserPasswordUseCase(userRepository, passwordHasher);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepository userRepository, AdministratorGuard administratorGuard) {
        return new DeleteUserUseCase(userRepository, administratorGuard);
    }
}
