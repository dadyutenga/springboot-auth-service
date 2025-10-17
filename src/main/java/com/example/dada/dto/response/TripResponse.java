package com.example.dada.dto.response;

import com.example.dada.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long riderId;
    private String riderName;
    private String pickupLocation;
    private String dropoffLocation;
    private BigDecimal distanceKm;
    private BigDecimal fare;
    private TripStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
