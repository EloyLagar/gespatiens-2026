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
                        .requestMatchers("/api/users/me", "/api/users/me/password").authenticated()
                        // RF7: editar, restablecer contraseñas y eliminar perfiles es cosa de la administración
                        .requestMatchers(HttpMethod.PUT, "/api/users/*", "/api/users/*/password").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMINISTRATOR")
                        // RF9: solo la administración puede dar de baja residentes
                        .requestMatchers(HttpMethod.POST, "/api/patients/*/discharge").hasRole("ADMINISTRATOR")
                        // El borrado definitivo es irreversible: también solo la administración
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/*").hasRole("ADMINISTRATOR")
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
