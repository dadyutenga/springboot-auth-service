package com.example.dada.service;

import com.example.dada.dto.RiderDto;
import com.example.dada.dto.TripResponseDto;
import com.example.dada.dto.UserDto;
import com.example.dada.dto.ReportResponseDto;
import com.example.dada.dto.RatingResponseDto;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.User;
import com.example.dada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RiderService riderService;
    private final TripService tripService;
    private final ReportService reportService;
    private final RatingService ratingService;

    /**
     * Retrieve all users and return them as DTOs.
     *
     * @return a list of UserDto representing every user in the repository
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userService::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Enable or disable the user account identified by the given ID.
     *
     * @param userId the UUID of the user to update
     * @param enabled true to enable the user, false to disable
     * @return the updated user as a UserDto
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public UserDto setUserEnabled(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(enabled);
        return userService.mapToDto(userRepository.save(user));
    }

    /**
     * Retrieves all riders as data-transfer objects.
     *
     * @return a list of RiderDto representing every rider
     */
    public List<RiderDto> getAllRiders() {
        return riderService.getAllRiders();
    }

    /**
     * Retrieve all trips as response DTOs.
     *
     * @return a List of TripResponseDto containing all trips
     */
    public List<TripResponseDto> getAllTrips() {
        return tripService.getAllTrips();
    }

    /**
     * Retrieves all reports as response DTOs.
     *
     * @return a List of ReportResponseDto representing all reports.
     */
    public List<ReportResponseDto> getReports() {
        return reportService.getAllReports();
    }

    /**
     * Marks the specified report as under review and returns the updated report.
     *
     * @param reportId the UUID of the report to mark under review
     * @return the updated report as a {@code ReportResponseDto} with its status set to under review
     */
    public ReportResponseDto markReportUnderReview(UUID reportId) {
        return reportService.markUnderReview(reportId);
    }

    /**
     * Resolve the report with the given identifier and return its updated representation.
     *
     * @param reportId the UUID of the report to resolve
     * @return the resolved report as a ReportResponseDto
     */
    public ReportResponseDto resolveReport(UUID reportId) {
        return reportService.resolveReport(reportId);
    }

    /**
     * Retrieve ratings associated with a specific user.
     *
     * @param userId the UUID of the user whose ratings are requested
     * @return a list of RatingResponseDto objects for the user, or an empty list if none exist
     */
    public List<RatingResponseDto> getRatingsForUser(UUID userId) {
        return ratingService.getRatingsForUser(userId);
    }
}
