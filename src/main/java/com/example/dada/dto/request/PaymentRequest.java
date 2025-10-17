package com.example.dada.dto.request;

import com.example.dada.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull(message = "Trip ID is required")
    private Long tripId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
    
    @NotNull(message = "Phone number is required for mobile payment")
    private String phoneNumber;
}
