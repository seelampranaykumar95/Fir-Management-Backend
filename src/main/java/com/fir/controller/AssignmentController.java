package com.fir.controller;

import java.util.List;

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

import com.fir.dto.CreateAssignmentRequest;
import com.fir.model.FirAssignment;
import com.fir.model.User;
import com.fir.service.AuditLogService;
import com.fir.service.FirAssignmentService;
import com.fir.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    private final FirAssignmentService firAssignmentService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public AssignmentController(
            FirAssignmentService firAssignmentService,
            UserService userService,
            AuditLogService auditLogService) {
        this.firAssignmentService = firAssignmentService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<FirAssignment> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        FirAssignment assignment = firAssignmentService.createAssignment(request, currentUser);
        auditLogService.recordAction(
                currentUser,
                "FIR_ASSIGNED",
                "FIR_ASSIGNMENT",
                assignment.getId(),
                "Assigned FIR " + assignment.getFir().getId() + " to officer " + assignment.getAssignedToOfficer().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @GetMapping("/{officerId}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<FirAssignment>> getAssignmentsByOfficer(
            @PathVariable Long officerId,
            Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(firAssignmentService.getAssignmentsByOfficerId(officerId, currentUser));
    }
}

