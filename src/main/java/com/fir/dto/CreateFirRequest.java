package com.fir.dto;

import com.fir.model.FirCategory;

import jakarta.validation.constraints.NotBlank;

public class CreateFirRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String location;

    private Long filedByUserId;

    private FirCategory category;

    private Long policeStationId;

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

    public Long getFiledByUserId() {
        return filedByUserId;
    }

    public void setFiledByUserId(Long filedByUserId) {
        this.filedByUserId = filedByUserId;
    }

    public FirCategory getCategory() {
        return category;
    }

    public void setCategory(FirCategory category) {
        this.category = category;
    }

    public Long getPoliceStationId() {
        return policeStationId;
    }

    public void setPoliceStationId(Long policeStationId) {
        this.policeStationId = policeStationId;
    }
}

