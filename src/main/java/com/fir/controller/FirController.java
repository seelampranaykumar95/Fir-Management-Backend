package com.fir.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fir.dto.CreateFirRequest;
import com.fir.dto.FirResponse;
import com.fir.dto.PageResponse;
import com.fir.dto.UpdateFirStatusRequest;
import com.fir.config.security.CustomUserPrincipal;
import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.model.FirStatus;
import com.fir.service.AuditLogService;
import com.fir.service.FirAcknowledgementPdfService;
import com.fir.service.FirService;
import com.fir.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/firs")
@PreAuthorize("hasAnyRole('ADMIN','CITIZEN','OFFICER')")
public class FirController {

    private final FirService firService;
    private final FirAcknowledgementPdfService firAcknowledgementPdfService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public FirController(
            FirService firService,
            FirAcknowledgementPdfService firAcknowledgementPdfService,
            UserService userService,
            AuditLogService auditLogService) {
        this.firService = firService;
        this.firAcknowledgementPdfService = firAcknowledgementPdfService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CITIZEN')")
    public ResponseEntity<FirResponse> createFir(@Valid @RequestBody CreateFirRequest request, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        if (!currentUser.getRole().equals(UserRole.ADMIN)) {
            request.setFiledByUserId(currentUser.getId());
        } else if (request.getFiledByUserId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!currentUser.getRole().equals(UserRole.ADMIN) && !currentUser.getId().equals(request.getFiledByUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Fir saved = firService.saveFir(request);
        auditLogService.recordAction(
                currentUser,
                "FIR_CREATED",
                "FIR",
                saved.getId(),
                "Created FIR in category " + saved.getCategory().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(FirResponse.fromEntity(saved));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<FirResponse>> getAllFirs(
            @RequestParam(required = false) FirStatus status,
            @RequestParam(required = false) FirCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(firService.getAllFirs(status, category, pageable).map(FirResponse::fromEntity)));
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<PageResponse<FirResponse>> getReviewQueue(
            @RequestParam(required = false) FirStatus status,
            @RequestParam(required = false) FirCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(firService.getReviewQueue(status, category, pageable).map(FirResponse::fromEntity)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CITIZEN')")
    public ResponseEntity<PageResponse<FirResponse>> getFirsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) FirStatus status,
            @RequestParam(required = false) FirCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        User currentUser = userService.getUserById(principal.getId());
        return ResponseEntity.ok(PageResponse.from(
                firService.getFirsByUserId(userId, status, category, pageable, currentUser).map(FirResponse::fromEntity)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','CITIZEN')")
    public ResponseEntity<PageResponse<FirResponse>> getMyFirs(
            @RequestParam(required = false) FirStatus status,
            @RequestParam(required = false) FirCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        User currentUser = userService.getUserById(principal.getId());
        return ResponseEntity.ok(PageResponse.from(
                firService.getFirsByUserId(currentUser.getId(), status, category, pageable, currentUser)
                        .map(FirResponse::fromEntity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FirResponse> getFirById(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        Fir fir = firService.getFirByIdForUser(id, currentUser);
        auditLogService.recordAction(
                currentUser,
                "FIR_VIEWED",
                "FIR",
                fir.getId(),
                "Viewed FIR details");
        return ResponseEntity.ok(FirResponse.fromEntity(fir));
    }

    @GetMapping("/{id}/acknowledgement")
    @PreAuthorize("hasAnyRole('ADMIN','CITIZEN')")
    public ResponseEntity<byte[]> downloadAcknowledgement(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        Fir fir = firService.getFirByIdForUser(id, currentUser);
        byte[] pdfBytes = firAcknowledgementPdfService.buildAcknowledgementPdf(fir);
        auditLogService.recordAction(
                currentUser,
                "FIR_ACKNOWLEDGEMENT_DOWNLOADED",
                "FIR",
                fir.getId(),
                "Downloaded FIR acknowledgement PDF");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fir-acknowledgement-" + fir.getId() + ".pdf")
                .body(pdfBytes);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<FirResponse> updateFirStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFirStatusRequest request,
            Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        Fir updated = firService.updateFirStatus(id, request, currentUser);
        auditLogService.recordAction(
                currentUser,
                "FIR_STATUS_UPDATED",
                "FIR",
                updated.getId(),
                "Changed FIR status to " + updated.getStatus().name());
        return ResponseEntity.ok(FirResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFir(@PathVariable Long id, Authentication authentication) {
        firService.deleteFir(id);
        auditLogService.recordAction(
                userService.getByEmail(authentication.getName()),
                "FIR_DELETED",
                "FIR",
                id,
                "Deleted FIR record");
        return ResponseEntity.noContent().build();
    }
}

