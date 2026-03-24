package com.fir.dto;

import jakarta.validation.constraints.NotNull;

public class CreateAssignmentRequest {

    @NotNull
    private Long firId;

    @NotNull
    private Long assignedToOfficerId;

    private Long assignedByUserId;

    public Long getFirId() {
        return firId;
    }

    public void setFirId(Long firId) {
        this.firId = firId;
    }

    public Long getAssignedToOfficerId() {
        return assignedToOfficerId;
    }

    public void setAssignedToOfficerId(Long assignedToOfficerId) {
        this.assignedToOfficerId = assignedToOfficerId;
    }

    public Long getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(Long assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }
}

