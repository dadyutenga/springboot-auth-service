package com.example.dada.dto.request;

import com.example.dada.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RiderRegistrationRequest {
    @NotBlank(message = "License number is required")
    private String licenseNumber;
    
    @NotBlank(message = "National ID is required")
    private String nationalId;
    
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
}
