package com.fir.dto;

import java.util.List;
import java.util.Map;

public class CrimeDashboardResponse {

    private Map<String, Long> summary;
    private List<ChartValueResponse> statusDistribution;
    private List<ChartValueResponse> categoryDistribution;
    private List<MonthlyTrendPointResponse> monthlyTrend;

    public Map<String, Long> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Long> summary) {
        this.summary = summary;
    }

    public List<ChartValueResponse> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(List<ChartValueResponse> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public List<ChartValueResponse> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<ChartValueResponse> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public List<MonthlyTrendPointResponse> getMonthlyTrend() {
        return monthlyTrend;
    }

    public void setMonthlyTrend(List<MonthlyTrendPointResponse> monthlyTrend) {
        this.monthlyTrend = monthlyTrend;
    }
}

