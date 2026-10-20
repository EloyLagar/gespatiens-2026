package com.viller.gespatiens.access.users.infrastructure.web;

// DTO de entrada de PUT /api/users/me. Sin email ni especialidad: el empleado no puede cambiarlos (RF4)
public record UpdateOwnProfileRequest(String name, String phoneNumber) {
}
