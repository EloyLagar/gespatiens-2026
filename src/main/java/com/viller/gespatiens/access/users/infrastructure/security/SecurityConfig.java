package com.viller.gespatiens.access.users.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CurrentUserJwtConverter currentUserJwtConverter) throws Exception {
        http
                // API REST sin sesión: cada petición trae su token en la cabecera Authorization,
                // así que no hay cookie de sesión que proteger contra CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        // El login es lo único accesible sin token
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Solo la administración puede dar de alta empleados
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMINISTRATOR")
                        // RF4: cada empleado gestiona su propio perfil. Va antes que la regla de /api/users/*
                        // porque Spring aplica la primera regla que encaja, y "me" también encajaría en "*"
                        .requestMatchers("/api/users/me", "/api/users/me/password", "/api/users/me/signature",
                                "/api/users/me/language").authenticated()
                        // La firma escaneada de otro empleado solo la ve y la cambia la administración
                        .requestMatchers("/api/users/*/signature").hasRole("ADMINISTRATOR")
                        // RF7: editar, restablecer contraseñas y eliminar perfiles es cosa de la administración
                        .requestMatchers(HttpMethod.PUT, "/api/users/*", "/api/users/*/password").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMINISTRATOR")
                        // Memoria final: "La aplicación deberá proporcionar a la administración un menú de gestión de
                        // usuarios": dar de alta, modificar, reincorporar, dar de baja (RF9) y borrar fichas, y
                        // gestionar a quién se autoriza a visitar, es de la administración. Consultarlas, de todos
                        .requestMatchers(HttpMethod.GET, "/api/patients/**").authenticated()
                        .requestMatchers("/api/patients", "/api/patients/**").hasRole("ADMINISTRATOR")
                        // Diario: "un espacio para que el equipo médico pueda registrar los pacientes rebajados
                        // con su tipo de rebaja. Los demás usuarios solo deben poder verlo". Por decisión del
                        // centro, la administración también puede registrarlas
                        .requestMatchers(HttpMethod.PUT, "/api/diary/*/reductions/*").hasAnyRole("MEDICAL_TEAM", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/diary/*/reductions/*").hasAnyRole("MEDICAL_TEAM", "ADMINISTRATOR")
                        // RF6: las notas y valoraciones de las actividades las pasan los educadores y la
                        // administración (decisión del centro). Los demás solo las consultan
                        .requestMatchers(HttpMethod.PUT, "/api/diary/activities/*/evaluations",
                                "/api/diary/activities/*/evaluations/*").hasAnyRole("SOCIAL_EDUCATOR", "ADMINISTRATOR")
                        // Los tutores de cada residente los asigna solo la administración (decisión del centro)
                        .requestMatchers(HttpMethod.PUT, "/api/interventions/residents/*/tutors").hasRole("ADMINISTRATOR")
                        // Cerrar y reabrir informes es cosa de la administración ("fecha y firma de la Dirección
                        // del recurso"). Qué área redacta cada uno lo decide el dominio
                        .requestMatchers(HttpMethod.POST, "/api/reports/*/close", "/api/reports/*/reopen").hasRole("ADMINISTRATOR")
                        // Memoria final, 5.1: un panel "al que solo esta [la administración] pudiese acceder"
                        .requestMatchers("/api/dashboard", "/api/dashboard/**").hasRole("ADMINISTRATOR")
                        // RF3: el resto de la aplicación solo para empleados autenticados
                        .anyRequest().authenticated())
                // Sin httpBasic: con él, el navegador abre su propia ventana de usuario y contraseña
                // cada vez que la API responde 401. El login va por POST /api/auth/login
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(currentUserJwtConverter)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
