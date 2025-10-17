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

    @PostMapping("/report")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReportResponseDto> createReport(@Valid @RequestBody ReportRequestDto request) {
        return ResponseEntity.ok(reportService.createReport(request));
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponseDto>> getReports() {
        return ResponseEntity.ok(adminService.getReports());
    }

    @PatchMapping("/admin/reports/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponseDto> markUnderReview(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.markReportUnderReview(id));
    }

    @PatchMapping("/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponseDto> resolveReport(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.resolveReport(id));
    }
}
