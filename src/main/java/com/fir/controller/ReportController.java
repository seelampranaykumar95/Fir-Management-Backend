package com.fir.controller;

import java.util.List;
import java.util.Map;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fir.dto.ChartValueResponse;
import com.fir.dto.CrimeDashboardResponse;
import com.fir.service.ReportService;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/fir-count")
    public ResponseEntity<Map<String, Object>> getFirCountReport() {
        return ResponseEntity.ok(reportService.getFirCountReport());
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyReport() {
        return ResponseEntity.ok(reportService.getMonthlyReport());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Long>> getStatusReport() {
        return ResponseEntity.ok(reportService.getStatusReport());
    }

    @GetMapping("/category")
    public ResponseEntity<Map<String, Long>> getCategoryReport() {
        return ResponseEntity.ok(reportService.getCategoryReport());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<CrimeDashboardResponse> getDashboardReport() {
        return ResponseEntity.ok(reportService.getDashboardReport());
    }
}

