package com.viller.gespatiens.access.users.application;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
