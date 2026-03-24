package com.fir.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fir.dto.ChartValueResponse;
import com.fir.dto.CrimeDashboardResponse;
import com.fir.dto.MonthlyTrendPointResponse;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;
import com.fir.repository.FirRepository;

@Service
public class ReportService {

    private final FirRepository firRepository;

    public ReportService(FirRepository firRepository) {
        this.firRepository = firRepository;
    }

    public Map<String, Object> getFirCountReport() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalFirs", firRepository.count());
        return response;
    }

    public List<Map<String, Object>> getMonthlyReport() {
        List<Map<String, Object>> report = new ArrayList<>();
        for (Object[] row : firRepository.fetchMonthlyCounts()) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", "%04d-%02d".formatted(year, month));
            monthData.put("count", ((Number) row[2]).longValue());
            report.add(monthData);
        }
        return report;
    }

    public Map<String, Long> getStatusReport() {
        Map<String, Long> report = new LinkedHashMap<>();
        for (FirStatus status : FirStatus.values()) {
            report.put(status.name(), 0L);
        }
        for (Object[] row : firRepository.fetchStatusCounts()) {
            String status = row[0] instanceof Enum<?> enumValue
                    ? enumValue.name()
                    : String.valueOf(row[0]);
            report.put(status, ((Number) row[1]).longValue());
        }
        return report;
    }

    public Map<String, Long> getCategoryReport() {
        Map<String, Long> report = new LinkedHashMap<>();
        for (FirCategory category : FirCategory.values()) {
            report.put(category.name(), 0L);
        }
        for (Object[] row : firRepository.fetchCategoryCounts()) {
            String category = row[0] instanceof Enum<?> enumValue
                    ? enumValue.name()
                    : row[0] == null ? FirCategory.OTHER.name() : String.valueOf(row[0]);
            report.put(category, ((Number) row[1]).longValue());
        }
        return report;
    }

    public CrimeDashboardResponse getDashboardReport() {
        Map<String, Long> statusReport = getStatusReport();
        Map<String, Long> categoryReport = getCategoryReport();

        CrimeDashboardResponse response = new CrimeDashboardResponse();
        response.setSummary(buildSummary(statusReport));
        response.setStatusDistribution(toChart(statusReport));
        response.setCategoryDistribution(toChart(categoryReport));
        response.setMonthlyTrend(getMonthlyReport().stream()
                .map(item -> new MonthlyTrendPointResponse(
                        String.valueOf(item.get("month")),
                        ((Number) item.get("count")).longValue()))
                .collect(Collectors.toList()));
        return response;
    }

    private Map<String, Long> buildSummary(Map<String, Long> statusReport) {
        Map<String, Long> summary = new LinkedHashMap<>();
        long totalFirs = statusReport.values().stream().mapToLong(Long::longValue).sum();
        long closedFirs = statusReport.getOrDefault(FirStatus.CLOSED.name(), 0L);
        long rejectedFirs = statusReport.getOrDefault(FirStatus.REJECTED.name(), 0L);
        long underReviewFirs = countStatuses(statusReport, List.of(
                FirStatus.SUBMITTED,
                FirStatus.PENDING_REVIEW,
                FirStatus.NEEDS_INFO,
                FirStatus.ACCEPTED));
        long investigationFirs = countStatuses(statusReport, List.of(FirStatus.ASSIGNED, FirStatus.INVESTIGATION));

        summary.put("totalFirs", totalFirs);
        summary.put("activeFirs", totalFirs - closedFirs - rejectedFirs);
        summary.put("underReviewFirs", underReviewFirs);
        summary.put("investigationFirs", investigationFirs);
        summary.put("closedFirs", closedFirs);
        summary.put("rejectedFirs", rejectedFirs);
        return summary;
    }

    private long countStatuses(Map<String, Long> statusReport, Collection<FirStatus> statuses) {
        return statuses.stream()
                .map(FirStatus::name)
                .mapToLong(status -> statusReport.getOrDefault(status, 0L))
                .sum();
    }

    private List<ChartValueResponse> toChart(Map<String, Long> report) {
        return report.entrySet().stream()
                .map(entry -> new ChartValueResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}

