package com.example.dada.repository;

import com.example.dada.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    /**
 * Finds a rating for a specific trip submitted by a specific reviewer.
 *
 * @param tripId     the UUID of the trip
 * @param reviewerId the UUID of the reviewer who submitted the rating
 * @return           an Optional containing the matching Rating if found, or empty otherwise
 */
Optional<Rating> findByTripIdAndReviewerId(UUID tripId, UUID reviewerId);
    /**
 * Retrieve all Rating entities for the specified target user.
 *
 * @param targetUserId the UUID of the user who received the ratings
 * @return a list of Rating objects associated with the given user; an empty list if none are found
 */
List<Rating> findByTargetUserId(UUID targetUserId);
}