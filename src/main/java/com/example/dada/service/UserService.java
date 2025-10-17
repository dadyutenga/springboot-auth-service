package com.example.dada.service;

import com.example.dada.dto.request.UpdateProfileRequest;
import com.example.dada.dto.response.UserProfileResponse;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.User;
import com.example.dada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    public UserProfileResponse getUserProfile() {
        User user = getCurrentUser();
        return mapToProfileResponse(user);
    }
    
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        user = userRepository.save(user);
        return mapToProfileResponse(user);
    }
    
    private UserProfileResponse mapToProfileResponse(User user) {
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
