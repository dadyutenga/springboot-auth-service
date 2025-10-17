package com.example.dada.service;

import com.example.dada.dto.request.RiderRegistrationRequest;
import com.example.dada.dto.response.RiderProfileResponse;
import com.example.dada.enums.RiderStatus;
import com.example.dada.enums.UserRole;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.RiderProfile;
import com.example.dada.model.User;
import com.example.dada.repository.RiderProfileRepository;
import com.example.dada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiderService {
    
    private final RiderProfileRepository riderProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    
    @Transactional
    public RiderProfileResponse registerRider(RiderRegistrationRequest request) {
        User user = userService.getCurrentUser();
        
        if (user.getRole() != UserRole.RIDER) {
            throw new BadRequestException("User must have RIDER role");
        }
        
        if (riderProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new BadRequestException("Rider profile already exists");
        }
        
        if (riderProfileRepository.findByLicenseNumber(request.getLicenseNumber()).isPresent()) {
            throw new BadRequestException("License number already registered");
        }
        
        if (riderProfileRepository.findByNationalId(request.getNationalId()).isPresent()) {
            throw new BadRequestException("National ID already registered");
        }
        
        RiderProfile riderProfile = RiderProfile.builder()
                .user(user)
                .licenseNumber(request.getLicenseNumber())
                .nationalId(request.getNationalId())
                .vehicleType(request.getVehicleType())
                .status(RiderStatus.PENDING)
                .earnings(BigDecimal.ZERO)
                .rating(BigDecimal.ZERO)
                .totalTrips(0)
                .build();
        
        riderProfile = riderProfileRepository.save(riderProfile);
        return mapToResponse(riderProfile);
    }
    
    public RiderProfileResponse getRiderProfile() {
        User user = userService.getCurrentUser();
        RiderProfile profile = riderProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        return mapToResponse(profile);
    }
    
    public List<RiderProfileResponse> getPendingRiders() {
        return riderProfileRepository.findByStatus(RiderStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void approveRider(Long riderId) {
        RiderProfile profile = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setStatus(RiderStatus.APPROVED);
        riderProfileRepository.save(profile);
    }
    
    @Transactional
    public void rejectRider(Long riderId) {
        RiderProfile profile = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setStatus(RiderStatus.REJECTED);
        riderProfileRepository.save(profile);
    }
    
    private RiderProfileResponse mapToResponse(RiderProfile profile) {
        return RiderProfileResponse.builder()
                .id(profile.getId())
                .licenseNumber(profile.getLicenseNumber())
                .nationalId(profile.getNationalId())
                .vehicleType(profile.getVehicleType())
                .status(profile.getStatus())
                .earnings(profile.getEarnings())
                .rating(profile.getRating())
                .totalTrips(profile.getTotalTrips())
                .build();
    }
}
