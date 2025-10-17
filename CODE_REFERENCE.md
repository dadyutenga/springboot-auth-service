# Boda Boda Delivery System - Complete Code Reference

This document contains all the code implementations needed to complete the system. Copy these files into your project.

---

## Table of Contents
1. [Service Implementations](#service-implementations)
2. [Controller Implementations](#controller-implementations)
3. [Configuration Files](#configuration-files)
4. [Repository Updates](#repository-updates)
5. [Application Configuration](#application-configuration)

---

## Service Implementations

### 1. TripService.java
**Location:** `src/main/java/com/example/dada/service/TripService.java`

```java
package com.example.dada.service;

import com.example.dada.dto.request.TripRequest;
import com.example.dada.dto.response.TripResponse;
import com.example.dada.enums.RiderStatus;
import com.example.dada.enums.TripStatus;
import com.example.dada.enums.UserRole;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.RiderProfile;
import com.example.dada.model.Trip;
import com.example.dada.model.User;
import com.example.dada.repository.RiderProfileRepository;
import com.example.dada.repository.TripRepository;
import com.example.dada.util.FareCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {
    
    private final TripRepository tripRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final UserService userService;
    private final FareCalculator fareCalculator;
    
    @Transactional
    public TripResponse createTrip(TripRequest request) {
        User customer = userService.getCurrentUser();
        
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new BadRequestException("Only customers can request trips");
        }
        
        BigDecimal fare = fareCalculator.calculateFare(request.getDistanceKm());
        
        Trip trip = Trip.builder()
                .customer(customer)
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .distanceKm(request.getDistanceKm())
                .fare(fare)
                .status(TripStatus.REQUESTED)
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .dropoffLatitude(request.getDropoffLatitude())
                .dropoffLongitude(request.getDropoffLongitude())
                .build();
        
        trip = tripRepository.save(trip);
        return mapToResponse(trip);
    }
    
    public List<TripResponse> getCustomerTrips() {
        User customer = userService.getCurrentUser();
        return tripRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TripResponse> getRiderTrips() {
        User rider = userService.getCurrentUser();
        return tripRepository.findByRiderId(rider.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TripResponse> getAvailableTrips() {
        User rider = userService.getCurrentUser();
        
        RiderProfile profile = riderProfileRepository.findByUserId(rider.getId())
                .orElseThrow(() -> new BadRequestException("Rider profile not found"));
        
        if (profile.getStatus() != RiderStatus.APPROVED) {
            throw new BadRequestException("Rider is not approved");
        }
        
        return tripRepository.findAvailableTrips(TripStatus.REQUESTED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TripResponse acceptTrip(Long tripId) {
        User rider = userService.getCurrentUser();
        
        RiderProfile profile = riderProfileRepository.findByUserId(rider.getId())
                .orElseThrow(() -> new BadRequestException("Rider profile not found"));
        
        if (profile.getStatus() != RiderStatus.APPROVED) {
            throw new BadRequestException("Rider is not approved");
        }
        
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        
        if (trip.getStatus() != TripStatus.REQUESTED) {
            throw new BadRequestException("Trip is not available");
        }
        
        trip.setRider(rider);
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(LocalDateTime.now());
        trip = tripRepository.save(trip);
        
        return mapToResponse(trip);
    }
    
    @Transactional
    public TripResponse startTrip(Long tripId) {
        User rider = userService.getCurrentUser();
        Trip trip = getTripForRider(tripId, rider.getId());
        
        if (trip.getStatus() != TripStatus.ACCEPTED) {
            throw new BadRequestException("Trip must be in ACCEPTED status");
        }
        
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip = tripRepository.save(trip);
        return mapToResponse(trip);
    }
    
    @Transactional
    public TripResponse completeTrip(Long tripId) {
        User rider = userService.getCurrentUser();
        Trip trip = getTripForRider(tripId, rider.getId());
        
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BadRequestException("Trip must be in progress");
        }
        
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(LocalDateTime.now());
        trip = tripRepository.save(trip);
        
        RiderProfile profile = riderProfileRepository.findByUserId(rider.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        
        BigDecimal riderEarnings = fareCalculator.calculateRiderEarnings(trip.getFare());
        profile.setEarnings(profile.getEarnings().add(riderEarnings));
        profile.setTotalTrips(profile.getTotalTrips() + 1);
        riderProfileRepository.save(profile);
        
        return mapToResponse(trip);
    }
    
    @Transactional
    public TripResponse cancelTrip(Long tripId) {
        User user = userService.getCurrentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        
        if (!trip.getCustomer().getId().equals(user.getId()) && 
            (trip.getRider() == null || !trip.getRider().getId().equals(user.getId()))) {
            throw new BadRequestException("Not authorized to cancel this trip");
        }
        
        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel completed trip");
        }
        
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledAt(LocalDateTime.now());
        trip = tripRepository.save(trip);
        
        return mapToResponse(trip);
    }
    
    public List<TripResponse> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private Trip getTripForRider(Long tripId, Long riderId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        
        if (trip.getRider() == null || !trip.getRider().getId().equals(riderId)) {
            throw new BadRequestException("Trip not assigned to this rider");
        }
        
        return trip;
    }
    
    private TripResponse mapToResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .customerId(trip.getCustomer().getId())
                .customerName(trip.getCustomer().getFullName())
                .riderId(trip.getRider() != null ? trip.getRider().getId() : null)
                .riderName(trip.getRider() != null ? trip.getRider().getFullName() : null)
                .pickupLocation(trip.getPickupLocation())
                .dropoffLocation(trip.getDropoffLocation())
                .distanceKm(trip.getDistanceKm())
                .fare(trip.getFare())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .acceptedAt(trip.getAcceptedAt())
                .completedAt(trip.getCompletedAt())
                .build();
    }
}
```

### 2. PaymentService.java
**Location:** `src/main/java/com/example/dada/service/PaymentService.java`

```java
package com.example.dada.service;

import com.example.dada.dto.request.PaymentRequest;
import com.example.dada.dto.response.PaymentResponse;
import com.example.dada.enums.PaymentStatus;
import com.example.dada.enums.TripStatus;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.Payment;
import com.example.dada.model.Trip;
import com.example.dada.model.User;
import com.example.dada.repository.PaymentRepository;
import com.example.dada.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final TripRepository tripRepository;
    private final UserService userService;
    
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        User user = userService.getCurrentUser();
        
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        
        if (!trip.getCustomer().getId().equals(user.getId())) {
            throw new BadRequestException("Not authorized to pay for this trip");
        }
        
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new BadRequestException("Trip must be completed before payment");
        }
        
        if (paymentRepository.findByTripId(trip.getId()).isPresent()) {
            throw new BadRequestException("Payment already exists for this trip");
        }
        
        Payment payment = Payment.builder()
                .trip(trip)
                .amount(trip.getFare())
                .method(request.getMethod())
                .phoneNumber(request.getPhoneNumber())
                .status(PaymentStatus.PENDING)
                .transactionId(generateTransactionId())
                .timestamp(LocalDateTime.now())
                .build();
        
        payment = paymentRepository.save(payment);
        
        // Mock payment processing
        processPayment(payment);
        
        return mapToResponse(payment);
    }
    
    private void processPayment(Payment payment) {
        // Mock implementation - in production, integrate with actual mobile money APIs
        log.info("Processing payment: {} via {}", payment.getTransactionId(), payment.getMethod());
        
        // Simulate async payment processing
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate API call delay
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setCompletedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Payment successful: {}", payment.getTransactionId());
            } catch (Exception e) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.error("Payment failed: {}", payment.getTransactionId(), e);
            }
        }).start();
    }
    
    public PaymentResponse getPaymentByTrip(Long tripId) {
        Payment payment = paymentRepository.findByTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapToResponse(payment);
    }
    
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapToResponse(payment);
    }
    
    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
    
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .tripId(payment.getTrip().getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .timestamp(payment.getTimestamp())
                .build();
    }
}
```

### 3. AdminService.java
**Location:** `src/main/java/com/example/dada/service/AdminService.java`

```java
package com.example.dada.service;

import com.example.dada.dto.response.AdminStatsResponse;
import com.example.dada.dto.response.UserProfileResponse;
import com.example.dada.enums.RiderStatus;
import com.example.dada.enums.TripStatus;
import com.example.dada.enums.UserRole;
import com.example.dada.model.User;
import com.example.dada.repository.PaymentRepository;
import com.example.dada.repository.RiderProfileRepository;
import com.example.dada.repository.TripRepository;
import com.example.dada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserRepository userRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final TripRepository tripRepository;
    private final PaymentRepository paymentRepository;
    
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }
    
    public List<UserProfileResponse> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role)
                .stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }
    
    public AdminStatsResponse getSystemStats() {
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        long totalRiders = userRepository.countByRole(UserRole.RIDER);
        long totalTrips = tripRepository.count();
        long completedTrips = tripRepository.findByStatus(TripStatus.COMPLETED).size();
        long pendingTrips = tripRepository.findByStatus(TripStatus.REQUESTED).size();
        long pendingRiderApprovals = riderProfileRepository.findByStatus(RiderStatus.PENDING).size();
        
        BigDecimal totalRevenue = tripRepository.findByStatus(TripStatus.COMPLETED)
                .stream()
                .map(trip -> trip.getFare())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalRiders(totalRiders)
                .totalTrips(totalTrips)
                .completedTrips(completedTrips)
                .pendingTrips(pendingTrips)
                .totalRevenue(totalRevenue)
                .pendingRiderApprovals(pendingRiderApprovals)
                .build();
    }
    
    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .verified(user.getVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
```

---

## Controller Implementations

### 1. TripController.java
**Location:** `src/main/java/com/example/dada/controller/TripController.java`

```java
package com.example.dada.controller;

import com.example.dada.dto.request.TripRequest;
import com.example.dada.dto.response.TripResponse;
import com.example.dada.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {
    
    private final TripService tripService;
    
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/my-trips")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TripResponse>> getMyTrips() {
        List<TripResponse> trips = tripService.getCustomerTrips();
        return ResponseEntity.ok(trips);
    }
    
    @GetMapping("/available")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<TripResponse>> getAvailableTrips() {
        List<TripResponse> trips = tripService.getAvailableTrips();
        return ResponseEntity.ok(trips);
    }
    
    @PostMapping("/{tripId}/accept")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<TripResponse> acceptTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.acceptTrip(tripId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{tripId}/start")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<TripResponse> startTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.startTrip(tripId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{tripId}/complete")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<TripResponse> completeTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.completeTrip(tripId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{tripId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RIDER')")
    public ResponseEntity<TripResponse> cancelTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.cancelTrip(tripId);
        return ResponseEntity.ok(response);
    }
}
```

### 2. RiderController.java
**Location:** `src/main/java/com/example/dada/controller/RiderController.java`

```java
package com.example.dada.controller;

import com.example.dada.dto.request.RiderRegistrationRequest;
import com.example.dada.dto.response.RiderProfileResponse;
import com.example.dada.dto.response.TripResponse;
import com.example.dada.service.RiderService;
import com.example.dada.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/riders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RIDER')")
public class RiderController {
    
    private final RiderService riderService;
    private final TripService tripService;
    
    @PostMapping("/register")
    public ResponseEntity<RiderProfileResponse> registerRider(
            @Valid @RequestBody RiderRegistrationRequest request) {
        RiderProfileResponse response = riderService.registerRider(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/profile")
    public ResponseEntity<RiderProfileResponse> getProfile() {
        RiderProfileResponse response = riderService.getRiderProfile();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-trips")
    public ResponseEntity<List<TripResponse>> getMyTrips() {
        List<TripResponse> trips = tripService.getRiderTrips();
        return ResponseEntity.ok(trips);
    }
    
    @GetMapping("/earnings")
    public ResponseEntity<RiderProfileResponse> getEarnings() {
        RiderProfileResponse response = riderService.getRiderProfile();
        return ResponseEntity.ok(response);
    }
}
```

### 3. AdminController.java
**Location:** `src/main/java/com/example/dada/controller/AdminController.java`

```java
package com.example.dada.controller;

import com.example.dada.dto.response.AdminStatsResponse;
import com.example.dada.dto.response.RiderProfileResponse;
import com.example.dada.dto.response.TripResponse;
import com.example.dada.dto.response.UserProfileResponse;
import com.example.dada.enums.UserRole;
import com.example.dada.service.AdminService;
import com.example.dada.service.RiderService;
import com.example.dada.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    private final RiderService riderService;
    private final TripService tripService;
    
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getSystemStats() {
        AdminStatsResponse stats = adminService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/customers")
    public ResponseEntity<List<UserProfileResponse>> getCustomers() {
        List<UserProfileResponse> customers = adminService.getUsersByRole(UserRole.CUSTOMER);
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/users/riders")
    public ResponseEntity<List<UserProfileResponse>> getRiders() {
        List<UserProfileResponse> riders = adminService.getUsersByRole(UserRole.RIDER);
        return ResponseEntity.ok(riders);
    }
    
    @GetMapping("/riders/pending")
    public ResponseEntity<List<RiderProfileResponse>> getPendingRiders() {
        List<RiderProfileResponse> riders = riderService.getPendingRiders();
        return ResponseEntity.ok(riders);
    }
    
    @PostMapping("/riders/{riderId}/approve")
    public ResponseEntity<Void> approveRider(@PathVariable Long riderId) {
        riderService.approveRider(riderId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/riders/{riderId}/reject")
    public ResponseEntity<Void> rejectRider(@PathVariable Long riderId) {
        riderService.rejectRider(riderId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/trips")
    public ResponseEntity<List<TripResponse>> getAllTrips() {
        List<TripResponse> trips = tripService.getAllTrips();
        return ResponseEntity.ok(trips);
    }
}
```

### 4. PaymentController.java
**Location:** `src/main/java/com/example/dada/controller/PaymentController.java`

```java
package com.example.dada.controller;

import com.example.dada.dto.request.PaymentRequest;
import com.example.dada.dto.response.PaymentResponse;
import com.example.dada.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/trip/{tripId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPaymentByTrip(@PathVariable Long tripId) {
        PaymentResponse response = paymentService.getPaymentByTrip(tripId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(@PathVariable String transactionId) {
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }
}
```

---

## Configuration Files

### 1. CorsConfig.java
**Location:** `src/main/java/com/example/dada/config/CorsConfig.java`

```java
package com.example.dada.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*")); // In production, specify actual origins
        config.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization",
                "Access-Control-Allow-Origin", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization",
                "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
```

### 2. OpenApiConfig.java
**Location:** `src/main/java/com/example/dada/config/OpenApiConfig.java`

```java
package com.example.dada.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Boda Boda