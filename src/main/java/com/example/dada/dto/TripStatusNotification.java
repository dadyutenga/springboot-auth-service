package com.example.dada.dto;

import com.example.dada.enums.TripStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TripStatusNotification(UUID tripId,
                                     UUID customerId,
                                     UUID riderId,
                                     TripStatus status,
                                     LocalDateTime timestamp) {
}
