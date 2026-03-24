package com.fir.dto;

import java.time.LocalDateTime;

import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;

public class FirResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private FirStatus status;
    private FirCategory category;
    private LocalDateTime createdAt;
    private Long filedByUserId;
    private String filedByName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public Long getFiledByUserId() {
        return filedByUserId;
    }

    public void setFiledByUserId(Long filedByUserId) {
        this.filedByUserId = filedByUserId;
    }

    public String getFiledByName() {
        return filedByName;
    }

    public void setFiledByName(String filedByName) {
        this.filedByName = filedByName;
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

    public static FirResponse fromEntity(Fir fir) {
        FirResponse response = new FirResponse();
        response.setId(fir.getId());
        response.setTitle(fir.getTitle());
        response.setDescription(fir.getDescription());
        response.setLocation(fir.getLocation());
        response.setStatus(fir.getStatus());
        response.setCategory(fir.getCategory());
        response.setCreatedAt(fir.getCreatedAt());
        if (fir.getFiledBy() != null) {
            response.setFiledByUserId(fir.getFiledBy().getId());
            response.setFiledByName(fir.getFiledBy().getName());
        }
        if (fir.getPoliceStation() != null) {
            response.setPoliceStationId(fir.getPoliceStation().getId());
            response.setPoliceStationName(fir.getPoliceStation().getName());
        }
        return response;
    }
}
