package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.domain.Speciality;

// DTO de entrada: lo que llega en el JSON del POST
public record RegisterUserRequest(
        String name,
        String email,
        String phoneNumber,
        Speciality speciality,
        String password) {
}
