package com.example.dada.controller;

import com.example.dada.dto.ReportRequestDto;
import com.example.dada.dto.ReportResponseDto;
import com.example.dada.service.AdminService;
import com.example.dada.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;
    private final AdminService adminService;

    /**
     * Create a new report from the provided request.
     *
     * @param request the DTO containing report details for creation
     * @return a ResponseEntity containing the created ReportResponseDto
     */
    @PostMapping("/report")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReportResponseDto> createReport(@Valid @RequestBody ReportRequestDto request) {
        return ResponseEntity.ok(reportService.createReport(request));
    }

    /**
     * Retrieves all reports available to administrators.
     *
     * @return a ResponseEntity containing a list of ReportResponseDto for all reports.
     */
    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponseDto>> getReports() {
        return ResponseEntity.ok(adminService.getReports());
    }

    /**
     * Mark a report as under review.
     *
     * @param id the UUID of the report to mark as under review
     * @return the updated ReportResponseDto reflecting the report's new under-review status
     */
    @PatchMapping("/admin/reports/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponseDto> markUnderReview(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.markReportUnderReview(id));
    }

    /**
     * Mark the report with the given id as resolved and return its updated representation.
     *
     * @param id the UUID of the report to resolve
     * @return the updated ReportResponseDto representing the resolved report
     */
    @PatchMapping("/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponseDto> resolveReport(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.resolveReport(id));
    }
}