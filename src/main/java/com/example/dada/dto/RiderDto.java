package com.example.dada.dto;

import com.example.dada.enums.RiderStatus;
import com.example.dada.enums.VehicleType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class RiderDto {
    UUID id;
    UUID userId;
    String fullName;
    String email;
    String licenseNumber;
    String nationalId;
    VehicleType vehicleType;
    RiderStatus status;
    BigDecimal totalEarnings;
    BigDecimal rating;
    String lastLocation;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
