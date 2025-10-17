package com.example.dada.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RatingRequestDto {
    @NotNull(message = "Trip ID is required")
    private UUID tripId;

    @NotNull(message = "Target user ID is required")
    private UUID targetUserId;

    @NotNull(message = "Rating value is required")
    @Min(1)
    @Max(5)
    private Integer ratingValue;

    private String comment;
}
