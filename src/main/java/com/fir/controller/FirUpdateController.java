package com.fir.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fir.dto.CreateFirUpdateRequest;
import com.fir.dto.CitizenInfoUpdateRequest;
import com.fir.dto.FirUpdateResponse;
import com.fir.model.FirUpdate;
import com.fir.model.User;
import com.fir.service.AuditLogService;
import com.fir.service.FirUpdateService;
import com.fir.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/fir-updates")
public class FirUpdateController {

    private final FirUpdateService firUpdateService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public FirUpdateController(
            FirUpdateService firUpdateService,
            UserService userService,
            AuditLogService auditLogService) {
        this.firUpdateService = firUpdateService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<FirUpdateResponse> createFirUpdate(
            @Valid @RequestBody CreateFirUpdateRequest request,
            Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        FirUpdate created = firUpdateService.createFirUpdate(request, currentUser);
        auditLogService.recordAction(
                currentUser,
                "FIR_UPDATE_CREATED",
                "FIR_UPDATE",
                created.getId(),
                "Added FIR update for FIR " + created.getFir().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FirUpdateResponse.fromEntity(created));
    }

    @PostMapping("/citizen-reply")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<FirUpdateResponse> createCitizenInfoUpdate(
            @Valid @RequestBody CitizenInfoUpdateRequest request,
            Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        FirUpdate created = firUpdateService.createCitizenInfoUpdate(request, currentUser);
        auditLogService.recordAction(
                currentUser,
                "CITIZEN_NEEDS_INFO_RESPONSE",
                "FIR_UPDATE",
                created.getId(),
                "Citizen responded to NEEDS_INFO for FIR " + created.getFir().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FirUpdateResponse.fromEntity(created));
    }

    @GetMapping("/{firId}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','CITIZEN')")
    public ResponseEntity<List<FirUpdateResponse>> getFirUpdatesByFirId(
            @PathVariable Long firId,
            Authentication authentication) {
        List<FirUpdateResponse> updates = firUpdateService
                .getFirUpdatesByFirId(firId, userService.getByEmail(authentication.getName()))
                .stream()
                .map(FirUpdateResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(updates);
    }
}

