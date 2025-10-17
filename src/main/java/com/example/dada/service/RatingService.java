package com.example.dada.service;

import com.example.dada.dto.RatingRequestDto;
import com.example.dada.dto.RatingResponseDto;
import com.example.dada.enums.TripStatus;
import com.example.dada.enums.UserRole;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.Rating;
import com.example.dada.model.RiderProfile;
import com.example.dada.model.Trip;
import com.example.dada.model.User;
import com.example.dada.repository.RatingRepository;
import com.example.dada.repository.RiderProfileRepository;
import com.example.dada.repository.TripRepository;
import com.example.dada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final UserService userService;

    /**
     * Submit a rating for a delivered trip from the current user to a specified target user.
     *
     * @param request DTO containing the tripId, targetUserId, ratingValue, and an optional comment
     * @return a RatingResponseDto representing the persisted rating
     * @throws ResourceNotFoundException if the trip or the target user cannot be found
     * @throws BadRequestException if the trip is not delivered, the current user is not a participant,
     *                             the current user has already rated the trip, or the target user is not
     *                             a valid recipient for the current user's role on the trip
     */
    @Transactional
    public RatingResponseDto submitRating(RatingRequestDto request) {
        User reviewer = userService.getCurrentUser();
        return submitRatingForUser(reviewer, request);
    }

    /**
     * Submit a rating on behalf of the supplied reviewer outside of the security context.
     *
     * @param reviewer the user submitting the rating
     * @param request  rating details containing the trip id, target user id and score
     * @return the persisted rating mapped to a response DTO
     */
    @Transactional
    public RatingResponseDto submitRatingForUser(User reviewer, RatingRequestDto request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (trip.getStatus() != TripStatus.DELIVERED) {
            throw new BadRequestException("Ratings can only be submitted after delivery");
        }

        if (!isParticipant(reviewer, trip)) {
            throw new BadRequestException("You are not allowed to rate this trip");
        }

        if (ratingRepository.findByTripIdAndReviewerId(trip.getId(), reviewer.getId()).isPresent()) {
            throw new BadRequestException("You have already rated this trip");
        }

        User target = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        validateTarget(reviewer, target, trip);

        Rating rating = Rating.builder()
                .trip(trip)
                .reviewer(reviewer)
                .targetUser(target)
                .ratingValue(request.getRatingValue())
                .comment(request.getComment())
                .build();

        Rating saved = ratingRepository.save(rating);
        updateAverages(target, request.getRatingValue());
        return mapToDto(saved);
    }

    /**
     * Retrieve all ratings submitted about a specific user.
     *
     * @param userId the UUID of the user whose received ratings will be returned
     * @return a list of RatingResponseDto representing ratings where the target user matches {@code userId}
     */
    public List<RatingResponseDto> getRatingsForUser(UUID userId) {
        return ratingRepository.findByTargetUserId(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert a Rating entity into a RatingResponseDto.
     *
     * @param rating the Rating entity to convert
     * @return a RatingResponseDto containing id, tripId, reviewerId, targetUserId, ratingValue, comment, and createdAt
     */
    public RatingResponseDto mapToDto(Rating rating) {
        return RatingResponseDto.builder()
                .id(rating.getId())
                .tripId(rating.getTrip().getId())
                .reviewerId(rating.getReviewer().getId())
                .targetUserId(rating.getTargetUser().getId())
                .ratingValue(rating.getRatingValue())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    /**
     * Checks whether the given user participated in the trip as its customer or assigned rider.
     *
     * @param user the user to check
     * @param trip the trip to inspect
     * @return {@code true} if the user is the trip's customer or its assigned rider, {@code false} otherwise
     */
    private boolean isParticipant(User user, Trip trip) {
        return trip.getCustomer().getId().equals(user.getId()) ||
                (trip.getRider() != null && trip.getRider().getId().equals(user.getId()));
    }

    /**
     * Validate that the reviewer is permitted to rate the specified target within the trip.
     *
     * @param reviewer the user submitting the rating (used to determine allowed role)
     * @param target   the user being rated
     * @param trip     the trip context used to verify relationships (assigned rider and customer)
     * @throws BadRequestException if a customer attempts to rate a user who is not the trip's assigned rider,
     *                             if a rider attempts to rate a user who is not the trip's customer,
     *                             or if the reviewer has a role other than CUSTOMER or RIDER
     */
    private void validateTarget(User reviewer, User target, Trip trip) {
        if (reviewer.getRole() == UserRole.CUSTOMER) {
            if (trip.getRider() == null || !trip.getRider().getId().equals(target.getId())) {
                throw new BadRequestException("Customers can only rate the assigned rider");
            }
        } else if (reviewer.getRole() == UserRole.RIDER) {
            if (!trip.getCustomer().getId().equals(target.getId())) {
                throw new BadRequestException("Riders can only rate the trip customer");
            }
        } else {
            throw new BadRequestException("Only riders and customers can submit ratings");
        }
    }

    /**
     * Recalculates and persists the target user's overall average rating and rating count, then
     * updates the rider profile average if the target user has the RIDER role.
     *
     * @param target the user whose averages will be updated
     * @param ratingValue the numeric rating to include in the recalculation
     */
    private void updateAverages(User target, Integer ratingValue) {
        int count = target.getRatingCount() == null ? 0 : target.getRatingCount();
        BigDecimal currentTotal = target.getRating() == null ? BigDecimal.ZERO : target.getRating().multiply(BigDecimal.valueOf(count));
        int newCount = count + 1;
        BigDecimal updatedAverage = currentTotal.add(BigDecimal.valueOf(ratingValue))
                .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);
        target.setRating(updatedAverage);
        target.setRatingCount(newCount);
        userRepository.save(target);

        if (target.getRole() == UserRole.RIDER) {
            riderProfileRepository.findByUserId(target.getId())
                    .ifPresent(profile -> updateRiderAverage(profile, ratingValue));
        }
    }

    /**
     * Updates the rider profile's average rating and rating count to include the given rating, then persists the profile.
     *
     * The new average is recalculated to incorporate the provided rating and is rounded to two decimal places using HALF_UP.
     *
     * @param profile     the RiderProfile to update and save
     * @param ratingValue the rating value to include in the profile's average
     */
    private void updateRiderAverage(RiderProfile profile, Integer ratingValue) {
        int count = profile.getRatingCount() == null ? 0 : profile.getRatingCount();
        BigDecimal currentTotal = profile.getRating() == null ? BigDecimal.ZERO : profile.getRating().multiply(BigDecimal.valueOf(count));
        int newCount = count + 1;
        BigDecimal updatedAverage = currentTotal.add(BigDecimal.valueOf(ratingValue))
                .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);
        profile.setRating(updatedAverage);
        profile.setRatingCount(newCount);
        riderProfileRepository.save(profile);
    }
}
