package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.audit.domain.AccessOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_log")
public class AccessEntryJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 254)
    private String email;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessOutcome outcome;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public AccessOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(AccessOutcome outcome) {
        this.outcome = outcome;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
