package com.fir.dto;

import com.fir.model.UpdateVisibility;

import jakarta.validation.constraints.NotBlank;

public class OfficerRemarkRequest {

    @NotBlank
    private String remark;

    private UpdateVisibility visibility;

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

