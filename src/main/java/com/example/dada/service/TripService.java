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

    /**
     * Create a new trip request for the currently authenticated customer.
     *
     * <p>Persists a Trip with status set to REQUESTED, publishes a status update, and returns the mapped response.
     *
     * @param request DTO containing pickup and dropoff locations, coordinates, distance, and fare for the trip
     * @return a TripResponseDto representing the saved trip with its initial status and timestamps
     */
    @Transactional
    public TripResponseDto createTrip(TripRequestDto request) {
        User customer = requireRole(UserRole.CUSTOMER);
        return createTripForUser(customer, request);
    }

    /**
     * Create a new trip request for the provided customer without relying on the security context.
     *
     * @param customer the customer initiating the trip request; must have the CUSTOMER role
     * @param request  trip details including pickup, dropoff, distance, and fare metadata
     * @return the persisted trip mapped to a response DTO
     */
    @Transactional
    public TripResponseDto createTripForUser(User customer, TripRequestDto request) {
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new BadRequestException("Operation allowed only for CUSTOMER role");
        }

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

    /**
     * Assigns the current rider to the specified trip, updates its status to ACCEPTED,
     * records the acceptance timestamp, persists the change, and publishes a status update.
     *
     * @param tripId the identifier of the trip to accept
     * @return a TripResponseDto representing the saved trip after acceptance
     * @throws ResourceNotFoundException if no trip exists with the given id
     * @throws BadRequestException if the current user is not a rider or the trip is not in REQUESTED status
     */
    @Transactional
    public TripResponseDto acceptTrip(UUID tripId) {
        User rider = requireRole(UserRole.RIDER);
        return acceptTripForUser(tripId, rider);
    }

    /**
     * Assign a rider to the specified trip without relying on the security context.
     *
     * @param tripId the identifier of the trip to accept
     * @param rider  the rider accepting the trip; must have the RIDER role
     * @return the updated trip mapped to a response DTO
     */
    @Transactional
    public TripResponseDto acceptTripForUser(UUID tripId, User rider) {
        if (rider.getRole() != UserRole.RIDER) {
            throw new BadRequestException("Operation allowed only for RIDER role");
        }
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

    /**
     * Updates a trip's status according to the provided request and returns the updated trip representation.
     *
     * The method applies cancellation semantics when the requested status is `CANCELLED`; otherwise it advances
     * the trip through the allowed status progression and may update rider-related metadata. The persisted trip
     * is published to the tracking publisher and returned as a DTO.
     *
     * @param tripId the identifier of the trip to update
     * @param request the status update request containing the desired `TripStatus` and optional rider location
     * @return a TripResponseDto reflecting the persisted trip after the status update
     * @throws ResourceNotFoundException if no trip exists with the given `tripId`
     * @throws BadRequestException if the update is not permitted (invalid transition, insufficient permissions, or other business rules)
     */
    @Transactional
    public TripResponseDto updateTripStatus(UUID tripId, TripStatusUpdateDto request) {
        User user = userService.getCurrentUser();
        return updateTripStatusForUser(tripId, user, request);
    }

    /**
     * Progress or cancel a trip on behalf of the supplied user.
     *
     * @param tripId  the trip identifier to update
     * @param user    the actor requesting the change
     * @param request the desired status update information
     * @return the persisted trip mapped to a response DTO
     */
    @Transactional
    public TripResponseDto updateTripStatusForUser(UUID tripId, User user, TripStatusUpdateDto request) {
        Trip trip = getTrip(tripId);
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

    /**
     * Retrieve trip details for the specified trip when the caller is either a participant (customer or assigned rider) or an admin.
     *
     * @param tripId the UUID of the trip to retrieve
     * @return a TripResponseDto representing the trip's current state and metadata
     * @throws com.example.dada.exception.ResourceNotFoundException if no trip exists for the given id
     * @throws com.example.dada.exception.BadRequestException if the current user is not a participant and not an admin
     */
    public TripResponseDto getTripDetails(UUID tripId) {
        User user = userService.getCurrentUser();
        return getTripDetailsForUser(tripId, user);
    }

    /**
     * Retrieve trip details for a user when invoked outside the security context.
     *
     * @param tripId the identifier of the trip
     * @param user   the user requesting access to the trip
     * @return the trip mapped to a response DTO
     */
    public TripResponseDto getTripDetailsForUser(UUID tripId, User user) {
        Trip trip = getTrip(tripId);
        if (!isParticipant(user, trip) && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Access denied for this trip");
        }
        return mapToDto(trip);
    }

    /**
     * Fetch the Trip entity when the supplied user participates in it.
     *
     * @param tripId the trip identifier
     * @param user   the requesting user
     * @return the Trip entity if the user is a participant or admin
     */
    public Trip getTripForParticipant(UUID tripId, User user) {
        Trip trip = getTrip(tripId);
        if (!isParticipant(user, trip) && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Access denied for this trip");
        }
        return trip;
    }

    /**
     * Retrieve all trips belonging to the currently authenticated customer.
     *
     * <p>Requires the current user to have the CUSTOMER role.</p>
     *
     * @throws BadRequestException if the current user's role is not CUSTOMER
     * @return a list of TripResponseDto representing the customer's trips
     */
    public List<TripResponseDto> getCustomerTrips() {
        User customer = requireRole(UserRole.CUSTOMER);
        return tripRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all trips assigned to the currently authenticated rider.
     *
     * This method requires the current user to have the RIDER role; it fetches trips
     * where the rider ID matches the authenticated user's ID and maps them to DTOs.
     *
     * @return a list of TripResponseDto objects for trips assigned to the current rider
     */
    public List<TripResponseDto> getRiderTrips() {
        User rider = requireRole(UserRole.RIDER);
        return tripRepository.findByRiderId(rider.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves trips that are currently open for pickup by riders.
     *
     * @return a list of TripResponseDto objects representing trips with status REQUESTED
     */
    public List<TripResponseDto> getAvailableTrips() {
        requireRole(UserRole.RIDER);
        return tripRepository.findAvailableTrips(TripStatus.REQUESTED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all trips in the system as response DTOs.
     *
     * @return a list of TripResponseDto objects representing every trip record
     */
    public List<TripResponseDto> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a Trip by its identifier.
     *
     * @param tripId the UUID of the trip to retrieve
     * @return the Trip with the specified id
     * @throws ResourceNotFoundException if no trip exists with the given id
     */
    private Trip getTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    /**
     * Cancels the given trip when requested by the trip's owning customer and the trip is still cancellable.
     *
     * @param user the user attempting the cancellation
     * @param trip the trip to cancel
     * @throws BadRequestException if the user is not the trip's customer or if the trip status is not REQUESTED or ACCEPTED
     */
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

    /**
     * Advance a trip to the specified next status and apply any related timestamp and rider-profile updates.
     *
     * Updates the trip's status (and acceptedAt/completedAt timestamps when applicable), and updates the assigned
     * rider's profile with an optional last location and, upon delivery, increments trip count and earnings.
     *
     * @param user the current user requesting the update
     * @param trip the trip to update
     * @param newStatus the desired next trip status; must match the allowed next status for the trip's current status
     * @param riderLocation optional rider location to persist on the rider profile; may be null
     * @throws BadRequestException if the trip has no assigned rider, the user is not the assigned rider or an admin,
     *         the current status cannot progress, or the requested status does not match the expected next status
     */
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

    /**
     * Checks whether a user is a participant in the given trip.
     *
     * @param user the user to check for participation
     * @param trip the trip to inspect
     * @return `true` if the user is the trip's customer or the assigned rider, `false` otherwise
     */
    private boolean isParticipant(User user, Trip trip) {
        return trip.getCustomer().getId().equals(user.getId()) ||
                (trip.getRider() != null && trip.getRider().getId().equals(user.getId()));
    }

    /**
     * Ensure the current authenticated user has the specified role.
     *
     * @param role the required user role
     * @return the current authenticated user when their role matches the required role
     * @throws BadRequestException if the current user's role does not match the required role
     */
    private User requireRole(UserRole role) {
        User user = userService.getCurrentUser();
        if (user.getRole() != role) {
            throw new BadRequestException("Operation allowed only for " + role + " role");
        }
        return user;
    }

    /**
     * Converts a Trip entity into a TripResponseDto.
     *
     * @param trip the Trip entity to map
     * @return a TripResponseDto containing the trip's id, customer and rider ids (rider id may be null),
     *         pickup and dropoff locations, distance (km), fare, status, and creation/update timestamps
     */
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
