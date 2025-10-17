package com.example.dada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReportRequestDto {
    @NotNull(message = "Trip ID is required")
    private UUID tripId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;
}
