package com.example.dada.repository;

import com.example.dada.enums.RiderStatus;
import com.example.dada.model.RiderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiderProfileRepository extends JpaRepository<RiderProfile, Long> {
    Optional<RiderProfile> findByUserId(Long userId);
    List<RiderProfile> findByStatus(RiderStatus status);
    Optional<RiderProfile> findByLicenseNumber(String licenseNumber);
    Optional<RiderProfile> findByNationalId(String nationalId);
}
