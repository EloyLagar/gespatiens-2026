package com.viller.gespatiens.access.users.infrastructure.security;

import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security llama a este servicio en cada login para cargar al empleado por su email
@Service
public class UserDetailsServiceAdapter implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // La especialidad hace de rol: ADMINISTRATOR -> ROLE_ADMINISTRATOR
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getSpeciality().name())
                .build();
    }
}
