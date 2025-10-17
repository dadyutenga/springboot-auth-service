package com.example.dada.controller;

import com.example.dada.dto.RatingRequestDto;
import com.example.dada.dto.RatingResponseDto;
import com.example.dada.service.RatingService;
import com.example.dada.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','RIDER')")
    public ResponseEntity<RatingResponseDto> submitRating(@Valid @RequestBody RatingRequestDto request) {
        return ResponseEntity.ok(ratingService.submitRating(request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RatingResponseDto>> getMyRatings() {
        UUID userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(ratingService.getRatingsForUser(userId));
    }
}
