package com.viller.gespatiens.diary.infrastructure.web;

import java.util.Set;
import java.util.UUID;

// DTO de entrada del diario
public final class DiaryRequests {

    private DiaryRequests() {
    }

    public record ShiftRequest(Set<UUID> staffIds, String notes) {
    }

    public record ReductionRequest(String partial, boolean total, boolean sport) {
    }
}
