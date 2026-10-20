package com.viller.gespatiens.access.users.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API REST sin sesión ni formularios: cada petición trae sus credenciales,
                // así que no hay cookie de sesión que proteger contra CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        // Solo la administración puede dar de alta empleados
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMINISTRATOR")
                        // RF9: solo la administración puede dar de baja residentes
                        .requestMatchers(HttpMethod.POST, "/api/patients/*/discharge").hasRole("ADMINISTRATOR")
                        // El borrado definitivo es irreversible: también solo la administración
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/*").hasRole("ADMINISTRATOR")
                        // RF3: el resto de la aplicación solo para empleados autenticados
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
