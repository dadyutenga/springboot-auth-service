package com.example.dada.whatsapp;

import com.example.dada.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents the conversational context for a WhatsApp chat session.
 *
 * <p>The state keeps track of the ongoing workflow (registration, trip request, etc.)
 * so that subsequent free-form messages can be interpreted correctly.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationState {

    public enum Stage {
        IDLE,
        AWAITING_EMAIL,
        AWAITING_NAME,
        AWAITING_ROLE,
        AWAITING_OTP,
        AWAITING_RIDE_CONFIRMATION,
        AWAITING_RIDER_DECISION,
        AWAITING_STATUS_UPDATE,
        AWAITING_RATING_COMMENT
    }

    @Builder.Default
    private Stage stage = Stage.IDLE;

    private String pendingEmail;
    private String pendingFullName;
    private UserRole pendingRole;
    private TemporaryCredential temporaryCredential;

    private String pendingPickup;
    private String pendingDropoff;
    private BigDecimal pendingDistance;
    private BigDecimal pendingFare;
    private UUID pendingTripId;

    private UUID pendingRatingTripId;
    private UUID pendingRatingTargetId;
    private Integer pendingRatingValue;
    private String pendingRatingComment;

    public static ConversationState idle() {
        return ConversationState.builder().stage(Stage.IDLE).build();
    }

    public void reset() {
        stage = Stage.IDLE;
        pendingEmail = null;
        pendingFullName = null;
        pendingRole = null;
        temporaryCredential = null;
        pendingPickup = null;
        pendingDropoff = null;
        pendingDistance = null;
        pendingFare = null;
        pendingTripId = null;
        pendingRatingTripId = null;
        pendingRatingTargetId = null;
        pendingRatingValue = null;
        pendingRatingComment = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemporaryCredential {
        private String hashedValue;
        private Instant expiresAt;
        private boolean used;

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }
}
