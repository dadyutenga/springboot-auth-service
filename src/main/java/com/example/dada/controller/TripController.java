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

    /**
     * Create a new trip for the authenticated customer.
     *
     * @param request the trip creation data transfer object containing pickup, destination, and fare details
     * @return the created TripResponseDto representing the newly created trip
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TripResponseDto> createTrip(@Valid @RequestBody TripRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(request));
    }

    /**
     * Retrieve trips belonging to the currently authenticated customer.
     *
     * @return a list of TripResponseDto representing the customer's trips
     */
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TripResponseDto>> getCustomerTrips() {
        return ResponseEntity.ok(tripService.getCustomerTrips());
    }

    /**
     * Retrieve trips assigned to the currently authenticated rider.
     *
     * @return the list of TripResponseDto objects for the authenticated rider
     */
    @GetMapping("/rider")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<TripResponseDto>> getRiderTrips() {
        return ResponseEntity.ok(tripService.getRiderTrips());
    }

    /**
     * Retrieve all trips that are currently available for riders to accept.
     *
     * @return a list of TripResponseDto representing trips available for assignment
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<TripResponseDto>> getAvailableTrips() {
        return ResponseEntity.ok(tripService.getAvailableTrips());
    }

    /**
     * Retrieves details for the trip identified by the given id.
     *
     * @param id the UUID of the trip to retrieve
     * @return the trip details as a TripResponseDto wrapped in the response entity
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TripResponseDto> getTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getTripDetails(id));
    }

    /**
     * Accepts a pending trip and assigns it to the calling rider.
     *
     * @param id the UUID of the trip to accept
     * @return the updated TripResponseDto for the accepted trip
     */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<TripResponseDto> acceptTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.acceptTrip(id));
    }

    /**
     * Update the status of an existing trip.
     *
     * @param id      the UUID of the trip to update
     * @param request the desired status update and any related metadata
     * @return the updated TripResponseDto representing the trip after the status change
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUSTOMER','RIDER','ADMIN')")
    public ResponseEntity<TripResponseDto> updateStatus(@PathVariable UUID id,
                                                        @Valid @RequestBody TripStatusUpdateDto request) {
        return ResponseEntity.ok(tripService.updateTripStatus(id, request));
    }
}
