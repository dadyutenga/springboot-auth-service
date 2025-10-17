package com.example.dada.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class RatingResponseDto {
    UUID id;
    UUID tripId;
    UUID reviewerId;
    UUID targetUserId;
    Integer ratingValue;
    String comment;
    LocalDateTime createdAt;
}
