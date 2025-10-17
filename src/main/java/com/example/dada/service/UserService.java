package com.example.dada.service;

import com.example.dada.dto.UserDto;
import com.example.dada.dto.request.UpdateProfileRequest;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.User;
import com.example.dada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Retrieve the currently authenticated user by the security context's principal email.
     *
     * @return the User that matches the authenticated principal's email
     * @throws ResourceNotFoundException if no user exists for the authenticated email
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Retrieve a user by phone number when present.
     *
     * @param phone the phone number to search for
     * @return an Optional containing the matching {@link User}
     */
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
    
    /**
     * Retrieve the authenticated user's profile as a UserDto.
     *
     * @return the authenticated user's profile populated as a {@link com.example.dada.dto.UserDto}
     */
    public UserDto getUserProfile() {
        User user = getCurrentUser();
        return mapToDto(user);
    }

    /**
     * Update the authenticated user's profile with values from the given request.
     *
     * @param request profile updates; `fullName` replaces the user's full name and `phone` replaces the user's phone only if non-null
     * @return the updated UserDto reflecting the persisted user data
     */
    @Transactional
    public UserDto updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        user.setFullName(request.getFullName());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        user = userRepository.save(user);
        return mapToDto(user);
    }

    /**
     * Convert a User entity to a UserDto.
     *
     * @param user the source User entity to map
     * @return a UserDto containing id, fullName, email, phone, role, enabled, verified, rating, createdAt, and updatedAt
     */
    public UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .verified(user.getVerified())
                .rating(user.getRating())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}