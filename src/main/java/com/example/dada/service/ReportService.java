package com.example.dada.service;

import com.example.dada.dto.ReportRequestDto;
import com.example.dada.dto.ReportResponseDto;
import com.example.dada.enums.ReportStatus;
import com.example.dada.exception.BadRequestException;
import com.example.dada.exception.ResourceNotFoundException;
import com.example.dada.model.Report;
import com.example.dada.model.Trip;
import com.example.dada.model.User;
import com.example.dada.repository.ReportRepository;
import com.example.dada.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final TripRepository tripRepository;
    private final UserService userService;

    /**
     * Create a new report for a trip submitted by the trip's customer.
     *
     * Validates that the current user is the trip's customer, persists a report with status PENDING,
     * and returns the saved report as a DTO.
     *
     * @param request the report request containing the trip ID, reason, and description
     * @return a ReportResponseDto representing the saved report
     * @throws ResourceNotFoundException if no trip exists with the provided ID
     * @throws BadRequestException if the current user is not the customer for the trip
     */
    @Transactional
    public ReportResponseDto createReport(ReportRequestDto request) {
        User reporter = userService.getCurrentUser();
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (!trip.getCustomer().getId().equals(reporter.getId())) {
            throw new BadRequestException("Only the customer can submit a report for this trip");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .trip(trip)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        return mapToDto(reportRepository.save(report));
    }

    /**
     * Retrieve all stored reports and convert them to response DTOs.
     *
     * @return a list of ReportResponseDto containing every report in the repository
     */
    public List<ReportResponseDto> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all reports currently marked as pending.
     *
     * @return a list of ReportResponseDto representing reports with status PENDING
     */
    public List<ReportResponseDto> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Marks the specified report's status as UNDER_REVIEW.
     *
     * @param reportId the UUID of the report to update
     * @return the updated report as a ReportResponseDto with status set to UNDER_REVIEW
     */
    @Transactional
    public ReportResponseDto markUnderReview(UUID reportId) {
        Report report = getReport(reportId);
        report.setStatus(ReportStatus.UNDER_REVIEW);
        return mapToDto(reportRepository.save(report));
    }

    /**
     * Mark the specified report as resolved.
     *
     * @param reportId the UUID of the report to resolve
     * @return the updated ReportResponseDto with status set to RESOLVED
     */
    @Transactional
    public ReportResponseDto resolveReport(UUID reportId) {
        Report report = getReport(reportId);
        report.setStatus(ReportStatus.RESOLVED);
        return mapToDto(reportRepository.save(report));
    }

    /**
     * Load the Report with the given id or throw a ResourceNotFoundException if none exists.
     *
     * @param reportId the UUID of the report to load
     * @return the Report with the given id
     * @throws ResourceNotFoundException if no report exists for the provided id
     */
    private Report getReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    /**
     * Convert a Report entity into a ReportResponseDto.
     *
     * @param report the Report entity to convert; its trip and reporter must be non-null
     * @return a ReportResponseDto containing id, tripId, reporterId, reporterEmail, reason, description, status, and createdAt
     */
    public ReportResponseDto mapToDto(Report report) {
        return ReportResponseDto.builder()
                .id(report.getId())
                .tripId(report.getTrip().getId())
                .reporterId(report.getReporter().getId())
                .reporterEmail(report.getReporter().getEmail())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}