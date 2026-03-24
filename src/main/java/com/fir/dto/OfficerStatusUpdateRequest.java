package com.fir.dto;

import com.fir.model.FirStatus;
import com.fir.model.UpdateVisibility;

import jakarta.validation.constraints.NotNull;

public class OfficerStatusUpdateRequest {

    @NotNull
    private FirStatus status;

    private String remark;

    private UpdateVisibility visibility;

    public FirStatus getStatus() {
        return status;
    }

    public void setStatus(FirStatus status) {
        this.status = status;
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

