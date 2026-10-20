package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.domain.Speciality;

// DTO de entrada de PUT /api/users/{id}: el perfil completo, sin contraseña
public record EditUserRequest(String name, String email, String phoneNumber, Speciality speciality) {
}
