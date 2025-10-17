package com.example.dada.controller;

import com.example.dada.dto.RatingResponseDto;
import com.example.dada.dto.RiderDto;
import com.example.dada.dto.TripResponseDto;
import com.example.dada.dto.UserDto;
import com.example.dada.service.AdminService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Retrieves all users.
     *
     * @return a ResponseEntity containing a list of UserDto for all users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    /**
     * Updates the enabled status of the user identified by the given id.
     *
     * @param id      UUID of the user to update
     * @param enabled true to enable the user, false to disable
     * @return the updated UserDto
     */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserDto> updateUserStatus(@PathVariable UUID id,
                                                    @RequestParam @NotNull Boolean enabled) {
        return ResponseEntity.ok(adminService.setUserEnabled(id, enabled));
    }

    /**
     * Retrieve all trips for administrative review.
     *
     * @return a list of TripResponseDto objects representing all trips
     */
    @GetMapping("/trips")
    public ResponseEntity<List<TripResponseDto>> getAllTrips() {
        return ResponseEntity.ok(adminService.getAllTrips());
    }

    /**
     * Retrieve all riders.
     *
     * @return a ResponseEntity containing a list of RiderDto representing all riders
     */
    @GetMapping("/riders")
    public ResponseEntity<List<RiderDto>> getAllRiders() {
        return ResponseEntity.ok(adminService.getAllRiders());
    }

    /**
     * Retrieves ratings for the specified user.
     *
     * @param userId UUID of the user whose ratings are requested.
     * @return a ResponseEntity containing a list of RatingResponseDto for the specified user.
     */
    @GetMapping("/ratings/{userId}")
    public ResponseEntity<List<RatingResponseDto>> getRatings(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getRatingsForUser(userId));
    }
}