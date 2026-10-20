package com.viller.gespatiens.access.users.infrastructure.web;

public record ChangeOwnPasswordRequest(String currentPassword, String newPassword) {
}
