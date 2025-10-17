package com.example.dada.dto;

import com.example.dada.enums.TripStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class TripResponseDto {
    UUID id;
    UUID customerId;
    UUID riderId;
    String pickupLocation;
    String dropoffLocation;
    BigDecimal distanceKm;
    BigDecimal fare;
    TripStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
