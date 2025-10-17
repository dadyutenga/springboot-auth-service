package com.example.dada.service;

import com.example.dada.dto.TripRequestDto;
import com.example.dada.dto.TripResponseDto;
import com.example.dada.dto.TripStatusUpdateDto;
import com.example.dada.enums.TripStatus;
import com.example.dada.enums.UserRole;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.Trip;
import com.example.dada.model.User;
import com.example.dada.repository.RiderProfileRepository;
import com.example.dada.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final Map<TripStatus, TripStatus> NEXT_STATUS = new EnumMap<>(TripStatus.class);

    static {
        NEXT_STATUS.put(TripStatus.REQUESTED, TripStatus.ACCEPTED);
        NEXT_STATUS.put(TripStatus.ACCEPTED, TripStatus.PICKED_UP);
        NEXT_STATUS.put(TripStatus.PICKED_UP, TripStatus.IN_TRANSIT);
        NEXT_STATUS.put(TripStatus.IN_TRANSIT, TripStatus.DELIVERED);
    }

    private final TripRepository tripRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final UserService userService;
    private final DeliveryTrackingPublisher trackingPublisher;

    @Transactional
    public TripResponseDto createTrip(TripRequestDto request) {
        User customer = requireRole(UserRole.CUSTOMER);

        Trip trip = Trip.builder()
                .customer(customer)
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .distanceKm(request.getDistanceKm())
                .fare(request.getFare())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .dropoffLatitude(request.getDropoffLatitude())
                .dropoffLongitude(request.getDropoffLongitude())
                .status(TripStatus.REQUESTED)
                .build();

        Trip saved = tripRepository.save(trip);
        trackingPublisher.publishStatusUpdate(saved);
        return mapToDto(saved);
    }

    @Transactional
    public TripResponseDto acceptTrip(UUID tripId) {
        User rider = requireRole(UserRole.RIDER);
        Trip trip = getTrip(tripId);

        if (trip.getStatus() != TripStatus.REQUESTED) {
            throw new BadRequestException("Trip is not available for acceptance");
        }

        trip.setRider(rider);
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(LocalDateTime.now());
        Trip saved = tripRepository.save(trip);
        trackingPublisher.publishStatusUpdate(saved);
        return mapToDto(saved);
    }

    @Transactional
    public TripResponseDto updateTripStatus(UUID tripId, TripStatusUpdateDto request) {
        Trip trip = getTrip(tripId);
        User user = userService.getCurrentUser();
        TripStatus newStatus = request.getStatus();

        if (newStatus == TripStatus.CANCELLED) {
            handleCancellation(user, trip);
        } else {
            handleProgressUpdate(user, trip, newStatus, request.getRiderLocation());
        }

        Trip saved = tripRepository.save(trip);
        trackingPublisher.publishStatusUpdate(saved);
        return mapToDto(saved);
    }

    public TripResponseDto getTripDetails(UUID tripId) {
        Trip trip = getTrip(tripId);
        User user = userService.getCurrentUser();
        if (!isParticipant(user, trip) && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Access denied for this trip");
        }
        return mapToDto(trip);
    }

    public List<TripResponseDto> getCustomerTrips() {
        User customer = requireRole(UserRole.CUSTOMER);
        return tripRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TripResponseDto> getRiderTrips() {
        User rider = requireRole(UserRole.RIDER);
        return tripRepository.findByRiderId(rider.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TripResponseDto> getAvailableTrips() {
        requireRole(UserRole.RIDER);
        return tripRepository.findAvailableTrips(TripStatus.REQUESTED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TripResponseDto> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private Trip getTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private void handleCancellation(User user, Trip trip) {
        if (user.getRole() != UserRole.CUSTOMER || !trip.getCustomer().getId().equals(user.getId())) {
            throw new BadRequestException("Only the customer can cancel this trip");
        }
        if (!(trip.getStatus() == TripStatus.REQUESTED || trip.getStatus() == TripStatus.ACCEPTED)) {
            throw new BadRequestException("Trip can no longer be cancelled");
        }
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledAt(LocalDateTime.now());
    }

    private void handleProgressUpdate(User user, Trip trip, TripStatus newStatus, String riderLocation) {
        if (trip.getRider() == null) {
            throw new BadRequestException("Trip has not been assigned to a rider yet");
        }
        if (!trip.getRider().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Only the assigned rider can update the trip");
        }
        TripStatus current = trip.getStatus();
        if (!NEXT_STATUS.containsKey(current)) {
            throw new BadRequestException("Trip cannot progress from status " + current);
        }
        TripStatus expected = NEXT_STATUS.get(current);
        if (newStatus != expected) {
            throw new BadRequestException("Invalid status transition from " + current + " to " + newStatus);
        }

        trip.setStatus(newStatus);
        if (newStatus == TripStatus.ACCEPTED) {
            trip.setAcceptedAt(LocalDateTime.now());
        }
        if (newStatus == TripStatus.DELIVERED) {
            trip.setCompletedAt(LocalDateTime.now());
        }

        riderProfileRepository.findByUserId(trip.getRider().getId())
                .ifPresent(profile -> {
                    if (riderLocation != null) {
                        profile.setLastLocation(riderLocation);
                    }
                    if (newStatus == TripStatus.DELIVERED) {
                        profile.setTotalTrips(profile.getTotalTrips() + 1);
                        profile.setTotalEarnings(profile.getTotalEarnings().add(trip.getFare()));
                    }
                    riderProfileRepository.save(profile);
                });
    }

    private boolean isParticipant(User user, Trip trip) {
        return trip.getCustomer().getId().equals(user.getId()) ||
                (trip.getRider() != null && trip.getRider().getId().equals(user.getId()));
    }

    private User requireRole(UserRole role) {
        User user = userService.getCurrentUser();
        if (user.getRole() != role) {
            throw new BadRequestException("Operation allowed only for " + role + " role");
        }
        return user;
    }

    public TripResponseDto mapToDto(Trip trip) {
        return TripResponseDto.builder()
                .id(trip.getId())
                .customerId(trip.getCustomer().getId())
                .riderId(trip.getRider() != null ? trip.getRider().getId() : null)
                .pickupLocation(trip.getPickupLocation())
                .dropoffLocation(trip.getDropoffLocation())
                .distanceKm(trip.getDistanceKm())
                .fare(trip.getFare())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}
