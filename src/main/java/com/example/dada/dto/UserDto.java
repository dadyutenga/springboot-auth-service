package com.example.dada.dto;

import com.example.dada.enums.UserRole;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class UserDto {
    UUID id;
    String fullName;
    String email;
    String phone;
    UserRole role;
    Boolean enabled;
    Boolean verified;
    BigDecimal rating;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
