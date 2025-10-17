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

    /**
     * Create a new rider profile for the currently authenticated rider user.
     *
     * Validates that the current user does not already have a rider profile and that the provided
     * license number and national ID are not already registered; then saves a new profile with
     * status set to PENDING and returns it as a RiderDto.
     *
     * @param request registration details including licenseNumber, nationalId, and vehicleType
     * @return the saved rider profile mapped to a RiderDto
     * @throws BadRequestException if the current user already has a rider profile, if the license
     *         number is already registered, or if the national ID is already registered
     */
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

    /**
     * Retrieve the current authenticated rider's profile.
     *
     * @return the rider's profile as a RiderDto
     * @throws ResourceNotFoundException if no rider profile exists for the current user
     * @throws BadRequestException if the current user does not have the RIDER role
     */
    public RiderDto getRiderProfile() {
        User user = requireRiderUser();
        return getRiderProfileForUser(user);
    }

    /**
     * Retrieve the rider profile for the provided user without relying on the security context.
     *
     * @param user the rider whose profile should be returned
     * @return the rider profile mapped to a DTO
     */
    public RiderDto getRiderProfileForUser(User user) {
        if (user.getRole() != UserRole.RIDER) {
            throw new BadRequestException("User must have RIDER role");
        }
        RiderProfile profile = riderProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        return mapToDto(profile);
    }

    /**
     * Retrieve all rider profiles that are in PENDING status.
     *
     * Each profile is converted to a RiderDto.
     *
     * @return the list of RiderDto objects representing rider profiles with status PENDING
     */
    public List<RiderDto> getPendingRiders() {
        return riderProfileRepository.findByStatus(RiderStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all rider profiles and convert them to DTOs.
     *
     * @return a list of RiderDto objects representing every rider profile
     */
    public List<RiderDto> getAllRiders() {
        return riderProfileRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Approves a rider profile by setting its status to APPROVED.
     *
     * @param riderId the UUID of the rider profile to approve
     * @throws ResourceNotFoundException if no rider profile with the given id exists
     */
    @Transactional
    public void approveRider(UUID riderId) {
        RiderProfile profile = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setStatus(RiderStatus.APPROVED);
        riderProfileRepository.save(profile);
    }

    /**
     * Marks the rider profile identified by the given id as rejected and persists the change.
     *
     * @param riderId the UUID of the rider profile to reject
     * @throws ResourceNotFoundException if no rider profile exists with the given id
     */
    @Transactional
    public void rejectRider(UUID riderId) {
        RiderProfile profile = riderProfileRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setStatus(RiderStatus.REJECTED);
        riderProfileRepository.save(profile);
    }

    /**
     * Convert a RiderProfile entity into a RiderDto.
     *
     * Copies the profile's id, associated user's id, full name and email, license number,
     * national ID, vehicle type, status, total earnings, rating, last known location,
     * and creation/update timestamps into a new RiderDto.
     *
     * @param profile the RiderProfile entity to convert
     * @return a RiderDto populated with values from the provided profile
     */
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

    /**
     * Obtain the current authenticated user and verify they have the RIDER role.
     *
     * @return the current authenticated User
     * @throws BadRequestException if the current user's role is not RIDER
     */
    private User requireRiderUser() {
        User user = userService.getCurrentUser();
        if (user.getRole() != UserRole.RIDER) {
            throw new BadRequestException("User must have RIDER role");
        }
        return user;
    }
}
