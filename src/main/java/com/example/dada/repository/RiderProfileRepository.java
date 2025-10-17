package com.example.dada.repository;

import com.example.dada.enums.RiderStatus;
import com.example.dada.model.RiderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiderProfileRepository extends JpaRepository<RiderProfile, UUID> {
    /**
 * Finds the rider profile associated with the given user identifier.
 *
 * @param userId the UUID of the user whose rider profile to find
 * @return an Optional containing the RiderProfile if one exists for the given userId, otherwise empty
 */
Optional<RiderProfile> findByUserId(UUID userId);
    /**
 * Finds rider profiles that have the specified status.
 *
 * @param status the RiderStatus to match
 * @return a list of RiderProfile objects with the specified status; empty if none match
 */
List<RiderProfile> findByStatus(RiderStatus status);
    /**
 * Finds a rider profile by driver's license number.
 *
 * @param licenseNumber the driver's license number to search for
 * @return an {@link Optional} containing the matching {@link RiderProfile}, or empty if none found
 */
Optional<RiderProfile> findByLicenseNumber(String licenseNumber);
    /**
 * Finds a rider profile by national ID.
 *
 * @param nationalId the national identification string to search for
 * @return an {@code Optional} containing the matching {@link RiderProfile} if found, or an empty {@code Optional} otherwise
 */
Optional<RiderProfile> findByNationalId(String nationalId);
}