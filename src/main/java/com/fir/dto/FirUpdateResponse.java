package com.fir.dto;

import java.time.LocalDateTime;

import com.fir.model.FirStatus;
import com.fir.model.FirUpdate;
import com.fir.model.UpdateVisibility;

public class FirUpdateResponse {

    private Long id;
    private Long firId;
    private Long updatedByUserId;
    private String updatedByName;
    private FirStatus statusAfter;
    private String remark;
    private UpdateVisibility visibility;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFirId() {
        return firId;
    }

    public void setFirId(Long firId) {
        this.firId = firId;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public FirStatus getStatusAfter() {
        return statusAfter;
    }

    public void setStatusAfter(FirStatus statusAfter) {
        this.statusAfter = statusAfter;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public UpdateVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(UpdateVisibility visibility) {
        this.visibility = visibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static FirUpdateResponse fromEntity(FirUpdate update) {
        FirUpdateResponse response = new FirUpdateResponse();
        response.setId(update.getId());
        response.setFirId(update.getFir().getId());
        response.setUpdatedByUserId(update.getUpdatedByUser().getId());
        response.setUpdatedByName(update.getUpdatedByUser().getName());
        response.setStatusAfter(update.getStatusAfter());
        response.setRemark(update.getRemark());
        response.setVisibility(update.getVisibility());
        response.setCreatedAt(update.getCreatedAt());
        return response;
    }
}

