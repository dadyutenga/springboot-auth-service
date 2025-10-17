package com.example.dada.service;

import com.example.dada.dto.RiderDto;
import com.example.dada.dto.request.RiderRegistrationRequest;
import com.example.dada.enums.RiderStatus;
import com.example.dada.enums.UserRole;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.RiderProfile;
import com.example.dada.model.User;
import com.example.dada.repository.RiderProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiderService {

    private final RiderProfileRepository riderProfileRepository;
    private final UserService userService;

    @Transactional
    public RiderDto registerRider(RiderRegistrationRequest request) {
        User user = requireRiderUser();

        riderProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            throw new BadRequestException("Rider profile already exists");
        });

        riderProfileRepository.findByLicenseNumber(request.getLicenseNumber()).ifPresent(profile -> {
            throw new BadRequestException("License number already registered");
        });

        riderProfileRepository.findByNationalId(request.getNationalId()).ifPresent(profile -> {
            throw new BadRequestException("National ID already registered");
        });

        RiderProfile riderProfile = RiderProfile.builder()
                .user(user)
                .licenseNumber(request.getLicenseNumber())
                .nationalId(request.getNationalId())
                .vehicleType(request.getVehicleType())
                .status(RiderStatus.PENDING)
                .build();

        return mapToDto(riderProfileRepository.save(riderProfile));
    }

    public RiderDto getRiderProfile() {
        User user = requireRiderUser();
        RiderProfile profile = riderProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        return mapToDto(profile);
    }

    public List<RiderDto> getPendingRiders() {
        return riderProfileRepository.findByStatus(RiderStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<RiderDto> getAllRiders() {
        return riderProfileRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveRider(UUID riderId) {
        RiderProfile profile = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setStatus(RiderStatus.APPROVED);
        riderProfileRepository.save(profile);
    }

    @Transactional
    public void rejectRider(UUID riderId) {
        RiderProfile profile = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setStatus(RiderStatus.REJECTED);
        riderProfileRepository.save(profile);
    }

    public RiderDto mapToDto(RiderProfile profile) {
        return RiderDto.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .fullName(profile.getUser().getFullName())
                .email(profile.getUser().getEmail())
                .licenseNumber(profile.getLicenseNumber())
                .nationalId(profile.getNationalId())
                .vehicleType(profile.getVehicleType())
                .status(profile.getStatus())
                .totalEarnings(profile.getTotalEarnings())
                .rating(profile.getRating())
                .lastLocation(profile.getLastLocation())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private User requireRiderUser() {
        User user = userService.getCurrentUser();
        if (user.getRole() != UserRole.RIDER) {
            throw new BadRequestException("User must have RIDER role");
        }
        return user;
    }
}
