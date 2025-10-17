package com.example.dada.dto.response;

import com.example.dada.enums.RiderStatus;
import com.example.dada.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderProfileResponse {
    private Long id;
    private String licenseNumber;
    private String nationalId;
    private VehicleType vehicleType;
    private RiderStatus status;
    private BigDecimal earnings;
    private BigDecimal rating;
    private Integer totalTrips;
}
