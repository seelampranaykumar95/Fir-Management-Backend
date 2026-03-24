package com.fir.dto;

public class MonthlyTrendPointResponse {

    private String month;
    private long count;

    public MonthlyTrendPointResponse() {
    }

    public MonthlyTrendPointResponse(String month, long count) {
        this.month = month;
        this.count = count;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

