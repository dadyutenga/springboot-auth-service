package com.example.dada.controller;

import com.example.dada.dto.UserDto;
import com.example.dada.dto.request.UpdateProfileRequest;
import com.example.dada.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Retrieve the authenticated user's profile.
     *
     * @return ResponseEntity containing the authenticated user's {@code UserDto} and HTTP 200 status.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    /**
     * Updates the authenticated user's profile using the provided data.
     *
     * @param request the validated profile update payload containing fields to change
     * @return the updated user profile as a UserDto
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }
}