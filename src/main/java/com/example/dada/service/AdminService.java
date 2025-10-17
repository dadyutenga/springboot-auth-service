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

    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userService::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto setUserEnabled(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(enabled);
        return userService.mapToDto(userRepository.save(user));
    }

    public List<RiderDto> getAllRiders() {
        return riderService.getAllRiders();
    }

    public List<TripResponseDto> getAllTrips() {
        return tripService.getAllTrips();
    }

    public List<ReportResponseDto> getReports() {
        return reportService.getAllReports();
    }

    public ReportResponseDto markReportUnderReview(UUID reportId) {
        return reportService.markUnderReview(reportId);
    }

    public ReportResponseDto resolveReport(UUID reportId) {
        return reportService.resolveReport(reportId);
    }

    public List<RatingResponseDto> getRatingsForUser(UUID userId) {
        return ratingService.getRatingsForUser(userId);
    }
}
