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

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserDto> updateUserStatus(@PathVariable UUID id,
                                                    @RequestParam @NotNull Boolean enabled) {
        return ResponseEntity.ok(adminService.setUserEnabled(id, enabled));
    }

    @GetMapping("/trips")
    public ResponseEntity<List<TripResponseDto>> getAllTrips() {
        return ResponseEntity.ok(adminService.getAllTrips());
    }

    @GetMapping("/riders")
    public ResponseEntity<List<RiderDto>> getAllRiders() {
        return ResponseEntity.ok(adminService.getAllRiders());
    }

    @GetMapping("/ratings/{userId}")
    public ResponseEntity<List<RatingResponseDto>> getRatings(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getRatingsForUser(userId));
    }
}
