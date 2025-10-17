package com.example.dada.controller;

import com.example.dada.dto.TripRequestDto;
import com.example.dada.dto.TripResponseDto;
import com.example.dada.dto.TripStatusUpdateDto;
import com.example.dada.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TripResponseDto> createTrip(@Valid @RequestBody TripRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(request));
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TripResponseDto>> getCustomerTrips() {
        return ResponseEntity.ok(tripService.getCustomerTrips());
    }

    @GetMapping("/rider")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<TripResponseDto>> getRiderTrips() {
        return ResponseEntity.ok(tripService.getRiderTrips());
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<TripResponseDto>> getAvailableTrips() {
        return ResponseEntity.ok(tripService.getAvailableTrips());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TripResponseDto> getTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getTripDetails(id));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<TripResponseDto> acceptTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.acceptTrip(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUSTOMER','RIDER','ADMIN')")
    public ResponseEntity<TripResponseDto> updateStatus(@PathVariable UUID id,
                                                        @Valid @RequestBody TripStatusUpdateDto request) {
        return ResponseEntity.ok(tripService.updateTripStatus(id, request));
    }
}
