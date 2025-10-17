package com.example.dada.repository;

import com.example.dada.enums.TripStatus;
import com.example.dada.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findByCustomerId(UUID customerId);
    List<Trip> findByRiderId(UUID riderId);
    List<Trip> findByStatus(TripStatus status);

    @Query("SELECT t FROM Trip t WHERE t.status = :status AND t.rider IS NULL")
    List<Trip> findAvailableTrips(@Param("status") TripStatus status);

    @Query("SELECT t FROM Trip t WHERE t.rider.id = :riderId AND t.status IN :statuses")
    List<Trip> findByRiderIdAndStatusIn(@Param("riderId") UUID riderId, @Param("statuses") List<TripStatus> statuses);

    Optional<Trip> findByIdAndCustomerId(UUID id, UUID customerId);
}
