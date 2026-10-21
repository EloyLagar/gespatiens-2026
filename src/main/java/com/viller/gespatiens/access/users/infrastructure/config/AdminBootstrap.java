package com.viller.gespatiens.access.users.infrastructure.config;

import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Problema del huevo y la gallina: solo un administrador puede crear usuarios,
// así que el primero se crea al arrancar a partir de variables de entorno
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final UserRepository userRepository;
    private final String email;
    private final String phoneNumber;
    private final String password;

    public AdminBootstrap(RegisterUserUseCase registerUserUseCase,
                          UserRepository userRepository,
                          @Value("${gespatiens.bootstrap-admin.email:}") String email,
                          @Value("${gespatiens.bootstrap-admin.phone-number:}") String phoneNumber,
                          @Value("${gespatiens.bootstrap-admin.password:}") String password) {
        this.registerUserUseCase = registerUserUseCase;
        this.userRepository = userRepository;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank()) {
            log.info("ADMIN_EMAIL no definido: no se crea administrador inicial");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }
        registerUserUseCase.execute("Administrador", email, phoneNumber, Speciality.ADMINISTRATOR, password);
        log.info("Administrador inicial creado: {}", email);
    }
}
