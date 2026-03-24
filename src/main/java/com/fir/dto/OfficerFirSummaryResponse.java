package com.fir.dto;

import java.time.LocalDateTime;

import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;

public class OfficerFirSummaryResponse {

    private Long id;
    private String title;
    private FirStatus status;
    private FirCategory category;
    private LocalDateTime createdAt;
    private Long policeStationId;
    private String policeStationName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public FirStatus getStatus() {
        return status;
    }

    public void setStatus(FirStatus status) {
        this.status = status;
    }

    public FirCategory getCategory() {
        return category;
    }

    public void setCategory(FirCategory category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getPoliceStationId() {
        return policeStationId;
    }

    public void setPoliceStationId(Long policeStationId) {
        this.policeStationId = policeStationId;
    }

    public String getPoliceStationName() {
        return policeStationName;
    }

    public void setPoliceStationName(String policeStationName) {
        this.policeStationName = policeStationName;
    }

    public static OfficerFirSummaryResponse fromEntity(Fir fir) {
        OfficerFirSummaryResponse response = new OfficerFirSummaryResponse();
        response.setId(fir.getId());
        response.setTitle(fir.getTitle());
        response.setStatus(fir.getStatus());
        response.setCategory(fir.getCategory());
        response.setCreatedAt(fir.getCreatedAt());
        if (fir.getPoliceStation() != null) {
            response.setPoliceStationId(fir.getPoliceStation().getId());
            response.setPoliceStationName(fir.getPoliceStation().getName());
        }
        return response;
    }
}
