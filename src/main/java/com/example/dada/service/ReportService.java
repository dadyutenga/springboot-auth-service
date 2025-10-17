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

    public List<ReportResponseDto> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ReportResponseDto> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReportResponseDto markUnderReview(UUID reportId) {
        Report report = getReport(reportId);
        report.setStatus(ReportStatus.UNDER_REVIEW);
        return mapToDto(reportRepository.save(report));
    }

    @Transactional
    public ReportResponseDto resolveReport(UUID reportId) {
        Report report = getReport(reportId);
        report.setStatus(ReportStatus.RESOLVED);
        return mapToDto(reportRepository.save(report));
    }

    private Report getReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

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
