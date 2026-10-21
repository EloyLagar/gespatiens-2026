package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.domain.Language;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;

import java.util.UUID;

// DTO de salida: nunca exponemos el agregado de dominio ni el hash de la contraseña
public record UserResponse(
        UUID id,
        String name,
        String email,
        String phoneNumber,
        Speciality speciality,
        // El idioma que ha elegido para la aplicación (RNF9)
        Language language) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getSpeciality(),
                user.getLanguage());
    }
}
