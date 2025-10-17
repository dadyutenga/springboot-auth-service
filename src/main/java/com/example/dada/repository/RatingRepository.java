package com.example.dada.repository;

import com.example.dada.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByTripIdAndReviewerId(UUID tripId, UUID reviewerId);
    List<Rating> findByTargetUserId(UUID targetUserId);
}
