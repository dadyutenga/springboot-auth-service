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

    @Transactional
    public RatingResponseDto submitRating(RatingRequestDto request) {
        User reviewer = userService.getCurrentUser();
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

    public List<RatingResponseDto> getRatingsForUser(UUID userId) {
        return ratingRepository.findByTargetUserId(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

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

    private boolean isParticipant(User user, Trip trip) {
        return trip.getCustomer().getId().equals(user.getId()) ||
                (trip.getRider() != null && trip.getRider().getId().equals(user.getId()));
    }

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
