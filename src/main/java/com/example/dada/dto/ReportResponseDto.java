package com.example.dada.dto;

import com.example.dada.enums.ReportStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ReportResponseDto {
    UUID id;
    UUID tripId;
    UUID reporterId;
    String reporterEmail;
    String reason;
    String description;
    ReportStatus status;
    LocalDateTime createdAt;
}
