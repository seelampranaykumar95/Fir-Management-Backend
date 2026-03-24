package com.fir.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fir.dto.EvidenceUploadResponse;
import com.fir.model.EvidenceFile;
import com.fir.model.User;
import com.fir.service.AuditLogService;
import com.fir.service.EvidenceService;
import com.fir.service.UserService;

@RestController
@RequestMapping("/evidence")
@PreAuthorize("hasAnyRole('ADMIN','OFFICER','CITIZEN')")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public EvidenceController(EvidenceService evidenceService, UserService userService, AuditLogService auditLogService) {
        this.evidenceService = evidenceService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenceUploadResponse> uploadEvidence(
             @RequestParam Long firId,
             @RequestParam(required = false) Long uploadedByUserId,
             Authentication authentication,
             @RequestParam("file") MultipartFile file) {
        User currentUser = userService.getByEmail(authentication.getName());
        EvidenceFile saved = evidenceService.uploadEvidence(firId, currentUser, file);
        auditLogService.recordAction(
                currentUser,
                "EVIDENCE_UPLOADED",
                "EVIDENCE_FILE",
                saved.getId(),
                "Uploaded file " + saved.getFileName() + " for FIR " + saved.getFir().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/{firId}")
    public ResponseEntity<List<EvidenceUploadResponse>> getEvidenceByFirId(
            @PathVariable Long firId,
            Authentication authentication) {
        List<EvidenceUploadResponse> evidenceFiles = evidenceService
                .getEvidenceByFirId(firId, userService.getByEmail(authentication.getName()))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(evidenceFiles);
    }

    @GetMapping("/{evidenceId}/download")
    public ResponseEntity<byte[]> downloadEvidence(
            @PathVariable Long evidenceId,
            Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        EvidenceFile evidenceFile = evidenceService.getEvidenceById(evidenceId, currentUser);
        byte[] content = evidenceService.downloadEvidence(evidenceId, currentUser);
        auditLogService.recordAction(
                currentUser,
                "EVIDENCE_DOWNLOADED",
                "EVIDENCE_FILE",
                evidenceFile.getId(),
                "Downloaded evidence file for FIR " + evidenceFile.getFir().getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evidenceFile.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + evidenceFile.getFileName() + "\"")
                .body(content);
    }

    private EvidenceUploadResponse toResponse(EvidenceFile evidenceFile) {
        EvidenceUploadResponse response = new EvidenceUploadResponse();
        response.setId(evidenceFile.getId());
        response.setFirId(evidenceFile.getFir().getId());
        response.setUploadedByUserId(evidenceFile.getUploadedByUser().getId());
        response.setFileName(evidenceFile.getFileName());
        response.setFileType(evidenceFile.getFileType());
        response.setFileSize(evidenceFile.getFileSize());
        response.setCreatedAt(evidenceFile.getCreatedAt());
        response.setDownloadUrl("/api/evidence/" + evidenceFile.getId() + "/download");
        return response;
    }
}

