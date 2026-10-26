package com.viller.gespatiens.access.users.infrastructure.config;

import com.viller.gespatiens.access.users.application.AccessTokenIssuer;
import com.viller.gespatiens.access.users.application.ChangeOwnLanguageUseCase;
import com.viller.gespatiens.access.users.application.ChangeOwnPasswordUseCase;
import com.viller.gespatiens.access.users.application.ChangeOwnThemeUseCase;
import com.viller.gespatiens.access.users.application.DeleteUserUseCase;
import com.viller.gespatiens.access.users.application.EditUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.InviteUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.LoginAttempts;
import com.viller.gespatiens.access.users.application.LoginUseCase;
import com.viller.gespatiens.access.users.application.MarkOwnTutorialSeenUseCase;
import com.viller.gespatiens.access.users.application.PasswordSetupLinkSender;
import com.viller.gespatiens.access.users.application.PasswordSetupLinks;
import com.viller.gespatiens.access.users.application.PasswordSetupUseCase;
import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.application.ResetUserPasswordUseCase;
import com.viller.gespatiens.access.users.application.SendPasswordSetupLinkUseCase;
import com.viller.gespatiens.access.users.application.SignatureUseCase;
import com.viller.gespatiens.access.users.application.UpdateOwnProfileUseCase;
import com.viller.gespatiens.access.users.domain.AdministratorGuard;
import com.viller.gespatiens.access.users.domain.PasswordHasher;
import com.viller.gespatiens.access.users.domain.PasswordSetupTokenRepository;
import com.viller.gespatiens.access.users.domain.SignatureRepository;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.access.users.infrastructure.mail.LoggingPasswordSetupLinkSender;
import com.viller.gespatiens.access.users.infrastructure.security.InMemoryLoginAttempts;
import com.viller.gespatiens.access.users.infrastructure.mail.MailPasswordSetupLinkSender;
import com.viller.gespatiens.access.users.infrastructure.mail.PasswordSetupLinkFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.Duration;

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

    // Con MAIL_HOST se envía un correo de verdad; sin él, el enlace sale en el log (para desarrollar)
    @Bean
    public PasswordSetupLinkSender passwordSetupLinkSender(ObjectProvider<JavaMailSender> mailSender,
                                                           @Value("${spring.mail.host:}") String mailHost,
                                                           @Value("${gespatiens.mail.from}") String from,
                                                           @Value("${gespatiens.frontend-url}") String frontendUrl) {
        PasswordSetupLinkFactory links = new PasswordSetupLinkFactory(frontendUrl);
        if (mailHost.isBlank()) {
            return new LoggingPasswordSetupLinkSender(links);
        }
        return new MailPasswordSetupLinkSender(mailSender.getObject(), links, from);
    }

    @Bean
    public PasswordSetupLinks passwordSetupLinks(PasswordSetupTokenRepository tokenRepository,
                                                 PasswordSetupLinkSender sender, Clock clock,
                                                 @Value("${gespatiens.password-setup.validity}") Duration validity) {
        return new PasswordSetupLinks(tokenRepository, sender, clock, validity);
    }

    @Bean
    public InviteUserUseCase inviteUserUseCase(UserRepository userRepository, PasswordSetupLinks passwordSetupLinks) {
        return new InviteUserUseCase(userRepository, passwordSetupLinks);
    }

    @Bean
    public SendPasswordSetupLinkUseCase sendPasswordSetupLinkUseCase(UserRepository userRepository,
                                                                     PasswordSetupLinks passwordSetupLinks) {
        return new SendPasswordSetupLinkUseCase(userRepository, passwordSetupLinks);
    }

    @Bean
    public PasswordSetupUseCase passwordSetupUseCase(PasswordSetupTokenRepository tokenRepository,
                                                     UserRepository userRepository, PasswordHasher passwordHasher,
                                                     Clock clock) {
        return new PasswordSetupUseCase(tokenRepository, userRepository, passwordHasher, clock);
    }

    @Bean
    public ChangeOwnLanguageUseCase changeOwnLanguageUseCase(UserRepository userRepository) {
        return new ChangeOwnLanguageUseCase(userRepository);
    }

    @Bean
    public ChangeOwnThemeUseCase changeOwnThemeUseCase(UserRepository userRepository) {
        return new ChangeOwnThemeUseCase(userRepository);
    }

    @Bean
    public MarkOwnTutorialSeenUseCase markOwnTutorialSeenUseCase(UserRepository userRepository,
                                                                 @Value("${gespatiens.tutorial.every-login}") boolean everyLogin) {
        return new MarkOwnTutorialSeenUseCase(userRepository, everyLogin);
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
                                     AccessTokenIssuer accessTokenIssuer, LoginAttempts loginAttempts, Clock clock) {
        return new LoginUseCase(userRepository, passwordHasher, accessTokenIssuer, loginAttempts, clock);
    }

    // Decisión del centro: al menos 5 intentos antes de bloquear
    @Bean
    public LoginAttempts loginAttempts(@Value("${gespatiens.login.max-attempts}") int maxAttempts,
                                       @Value("${gespatiens.login.lockout}") Duration lockout) {
        return new InMemoryLoginAttempts(maxAttempts, lockout);
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
