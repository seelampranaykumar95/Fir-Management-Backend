package com.fir.dto;

import com.fir.model.FirStatus;
import com.fir.model.UpdateVisibility;

import jakarta.validation.constraints.NotNull;

public class UpdateFirStatusRequest {

    @NotNull
    private FirStatus status;

    private Long updatedByUserId;

    private String remark;

    private UpdateVisibility visibility;

    public FirStatus getStatus() {
        return status;
    }

    public void setStatus(FirStatus status) {
        this.status = status;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
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
}

