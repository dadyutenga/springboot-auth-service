package com.example.dada.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private Long totalUsers;
    private Long totalCustomers;
    private Long totalRiders;
    private Long totalTrips;
    private Long completedTrips;
    private Long pendingTrips;
    private BigDecimal totalRevenue;
    private Long pendingRiderApprovals;
}
