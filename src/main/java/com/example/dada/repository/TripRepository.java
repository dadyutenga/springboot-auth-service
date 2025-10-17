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
    /**
 * Finds all trips belonging to the specified customer.
 *
 * @param customerId the UUID of the customer whose trips should be retrieved
 * @return a list of Trip entities associated with the given customer; empty list if none found
 */
List<Trip> findByCustomerId(UUID customerId);
    /**
 * Finds all trips associated with the specified rider.
 *
 * @param riderId the UUID of the rider whose trips to retrieve
 * @return a list of Trip entities belonging to the rider; empty list if none found
 */
List<Trip> findByRiderId(UUID riderId);
    /**
 * Finds all trips that have the specified status.
 *
 * @param status the trip status to filter by
 * @return a list of trips with the given status, or an empty list if none match
 */
List<Trip> findByStatus(TripStatus status);

    /**
     * Finds trips with the specified status that have no assigned rider.
     *
     * @param status the trip status to filter by
     * @return a list of trips matching the status and with no rider assigned
     */
    @Query("SELECT t FROM Trip t WHERE t.status = :status AND t.rider IS NULL")
    List<Trip> findAvailableTrips(@Param("status") TripStatus status);

    /**
     * Finds trips assigned to the specified rider that have a status contained in the provided list.
     *
     * @param riderId  the UUID of the rider whose trips are to be retrieved
     * @param statuses the list of trip statuses to match
     * @return         a list of Trip entities matching the rider ID and any of the given statuses; may be empty
     */
    @Query("SELECT t FROM Trip t WHERE t.rider.id = :riderId AND t.status IN :statuses")
    List<Trip> findByRiderIdAndStatusIn(@Param("riderId") UUID riderId, @Param("statuses") List<TripStatus> statuses);

    /**
 * Locate a trip by its id only if it belongs to the specified customer.
 *
 * @param id         the UUID of the trip to find
 * @param customerId the UUID of the customer who must own the trip
 * @return           an Optional containing the Trip if it exists and is owned by the customer, {@code Optional.empty()} otherwise
 */
Optional<Trip> findByIdAndCustomerId(UUID id, UUID customerId);
}