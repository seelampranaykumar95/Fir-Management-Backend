package com.fir.dto;

import com.fir.model.FirStatus;
import com.fir.model.UpdateVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateFirUpdateRequest {

    @NotNull
    private Long firId;

    private Long updatedByUserId;

    private FirStatus statusAfter;

    @NotBlank
    private String remark;

    private UpdateVisibility visibility;

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
}

