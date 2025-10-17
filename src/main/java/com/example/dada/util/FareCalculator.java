package com.example.dada.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FareCalculator {
    
    private static final BigDecimal BASE_FARE = new BigDecimal("1.50");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("0.75");
    private static final BigDecimal MINIMUM_FARE = new BigDecimal("2.00");
    
    public BigDecimal calculateFare(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return MINIMUM_FARE;
        }
        
        BigDecimal fare = BASE_FARE.add(distanceKm.multiply(PER_KM_RATE));
        fare = fare.setScale(2, RoundingMode.HALF_UP);
        
        if (fare.compareTo(MINIMUM_FARE) < 0) {
            return MINIMUM_FARE;
        }
        
        return fare;
    }
    
    public BigDecimal calculateRiderEarnings(BigDecimal fare) {
        // Rider gets 80% of the fare
        BigDecimal commissionRate = new BigDecimal("0.80");
        return fare.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
    }
}
