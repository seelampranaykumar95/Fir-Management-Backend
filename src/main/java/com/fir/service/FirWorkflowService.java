package com.fir.service;

import java.util.EnumSet;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fir.model.FirStatus;

@Service
public class FirWorkflowService {

    private static final EnumSet<FirStatus> REVIEW_STATUSES = EnumSet.of(
            FirStatus.SUBMITTED,
            FirStatus.PENDING_REVIEW,
            FirStatus.NEEDS_INFO,
            FirStatus.ACCEPTED);

    private static final EnumSet<FirStatus> REASSIGNABLE_STATUSES = EnumSet.of(
            FirStatus.ACCEPTED,
            FirStatus.ASSIGNED,
            FirStatus.INVESTIGATION);

    private static final Map<FirStatus, EnumSet<FirStatus>> ALLOWED_TRANSITIONS = Map.of(
            FirStatus.SUBMITTED, EnumSet.of(FirStatus.PENDING_REVIEW),
            FirStatus.PENDING_REVIEW, EnumSet.of(FirStatus.NEEDS_INFO, FirStatus.ACCEPTED, FirStatus.REJECTED),
            FirStatus.NEEDS_INFO, EnumSet.of(FirStatus.PENDING_REVIEW),
            FirStatus.ACCEPTED, EnumSet.of(FirStatus.ASSIGNED),
            FirStatus.ASSIGNED, EnumSet.of(FirStatus.INVESTIGATION),
            FirStatus.INVESTIGATION, EnumSet.of(FirStatus.CLOSED),
            FirStatus.CLOSED, EnumSet.noneOf(FirStatus.class),
            FirStatus.REJECTED, EnumSet.noneOf(FirStatus.class));

    public EnumSet<FirStatus> getReviewStatuses() {
        return EnumSet.copyOf(REVIEW_STATUSES);
    }

    public boolean isReviewStatus(FirStatus status) {
        return REVIEW_STATUSES.contains(status);
    }

    public boolean canBeAssigned(FirStatus status) {
        return REASSIGNABLE_STATUSES.contains(status);
    }

    public void validateTransition(FirStatus currentStatus, FirStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        EnumSet<FirStatus> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid FIR status transition from " + currentStatus + " to " + targetStatus);
        }
    }

    public void validateOfficerReviewTransition(FirStatus currentStatus, FirStatus targetStatus) {
        if (!isReviewStatus(currentStatus) || !isReviewStatus(targetStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Officers can only review FIRs in pre-assignment stages");
        }
        validateTransition(currentStatus, targetStatus);
    }

    public void validateOfficerAssignedTransition(FirStatus currentStatus, FirStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        if (currentStatus == FirStatus.ASSIGNED && targetStatus == FirStatus.INVESTIGATION) {
            return;
        }
        if (currentStatus == FirStatus.INVESTIGATION && targetStatus == FirStatus.CLOSED) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Assigned officers can only move FIRs from ASSIGNED to INVESTIGATION or INVESTIGATION to CLOSED");
    }
}

