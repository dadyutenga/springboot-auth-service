package com.example.dada.dto;

import com.example.dada.enums.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TripStatusUpdateDto {
    @NotNull(message = "Trip status is required")
    private TripStatus status;

    private String riderLocation;
}
