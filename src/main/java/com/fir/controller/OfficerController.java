package com.fir.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fir.dto.PageResponse;
import com.fir.dto.OfficerFirSummaryResponse;
import com.fir.dto.OfficerRemarkRequest;
import com.fir.dto.OfficerStatusUpdateRequest;
import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;
import com.fir.model.FirUpdate;
import com.fir.model.User;
import com.fir.service.AuditLogService;
import com.fir.service.OfficerService;
import com.fir.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/officers")
@PreAuthorize("hasRole('OFFICER')")
public class OfficerController {

    private final OfficerService officerService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public OfficerController(OfficerService officerService, UserService userService, AuditLogService auditLogService) {
        this.officerService = officerService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/{officerId}/firs")
    public ResponseEntity<PageResponse<OfficerFirSummaryResponse>> getAssignedFirs(
            @PathVariable Long officerId,
            @RequestParam(required = false) FirStatus status,
            @RequestParam(required = false) FirCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        validateOfficerAccess(officerId, authentication);
        return ResponseEntity.ok(PageResponse.from(
                officerService.getAssignedFirs(officerId, status, category, pageable)
                        .map(OfficerFirSummaryResponse::fromEntity)));
    }

    @PutMapping("/{officerId}/firs/{firId}/status")
    public ResponseEntity<Fir> updateAssignedFirStatus(
            @PathVariable Long officerId,
            @PathVariable Long firId,
            @Valid @RequestBody OfficerStatusUpdateRequest request,
            Authentication authentication) {
        validateOfficerAccess(officerId, authentication);
        User currentUser = userService.getByEmail(authentication.getName());
        Fir updated = officerService.updateAssignedFirStatus(officerId, firId, request);
        auditLogService.recordAction(
                currentUser,
                "OFFICER_FIR_STATUS_UPDATED",
                "FIR",
                updated.getId(),
                "Officer changed FIR status to " + updated.getStatus().name());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{officerId}/firs/{firId}/remarks")
    public ResponseEntity<FirUpdate> addRemark(
            @PathVariable Long officerId,
            @PathVariable Long firId,
            @Valid @RequestBody OfficerRemarkRequest request,
            Authentication authentication) {
        validateOfficerAccess(officerId, authentication);
        User currentUser = userService.getByEmail(authentication.getName());
        FirUpdate created = officerService.addRemark(officerId, firId, request);
        auditLogService.recordAction(
                currentUser,
                "OFFICER_REMARK_ADDED",
                "FIR_UPDATE",
                created.getId(),
                "Officer added remark for FIR " + firId);
        return ResponseEntity.ok(created);
    }

    private void validateOfficerAccess(Long officerId, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        if (!currentUser.getId().equals(officerId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to act on behalf of another officer");
        }
    }
}

