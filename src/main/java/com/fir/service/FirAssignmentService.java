package com.fir.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fir.dto.CreateAssignmentRequest;
import com.fir.model.Fir;
import com.fir.model.FirAssignment;
import com.fir.model.FirStatus;
import com.fir.model.FirUpdate;
import com.fir.model.UpdateVisibility;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.FirAssignmentRepository;
import com.fir.repository.FirRepository;
import com.fir.repository.FirUpdateRepository;
import com.fir.repository.UserRepository;

@Service
public class FirAssignmentService {

    private final FirAssignmentRepository firAssignmentRepository;
    private final FirRepository firRepository;
    private final UserRepository userRepository;
    private final FirUpdateRepository firUpdateRepository;
    private final FirWorkflowService firWorkflowService;

    public FirAssignmentService(
            FirAssignmentRepository firAssignmentRepository,
            FirRepository firRepository,
            UserRepository userRepository,
            FirUpdateRepository firUpdateRepository,
            FirWorkflowService firWorkflowService) {
        this.firAssignmentRepository = firAssignmentRepository;
        this.firRepository = firRepository;
        this.userRepository = userRepository;
        this.firUpdateRepository = firUpdateRepository;
        this.firWorkflowService = firWorkflowService;
    }

    @Transactional
    public FirAssignment createAssignment(CreateAssignmentRequest request, User assignedBy) {
        if (assignedBy.getRole() != UserRole.ADMIN && assignedBy.getRole() != UserRole.OFFICER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only police staff can assign FIRs");
        }

        Fir fir = firRepository.findById(request.getFirId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "FIR not found with id: " + request.getFirId()));
        if (!firWorkflowService.canBeAssigned(fir.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ACCEPTED, ASSIGNED, or INVESTIGATION FIRs can be assigned or reassigned");
        }

        User officer = userRepository.findById(request.getAssignedToOfficerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Officer not found with id: " + request.getAssignedToOfficerId()));
        if (officer.getRole() != UserRole.OFFICER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user does not have OFFICER role");
        }

        firAssignmentRepository.findByFirIdAndActiveTrue(fir.getId()).ifPresent(existingAssignment -> {
            existingAssignment.setActive(false);
            firAssignmentRepository.save(existingAssignment);
        });

        FirAssignment assignment = new FirAssignment();
        assignment.setFir(fir);
        assignment.setAssignedToOfficer(officer);
        assignment.setAssignedByUser(assignedBy);
        assignment.setActive(true);

        FirAssignment savedAssignment = firAssignmentRepository.save(assignment);

        boolean firstAssignment = fir.getStatus() == FirStatus.ACCEPTED;
        FirStatus statusAfterAssignment = firstAssignment ? FirStatus.ASSIGNED : fir.getStatus();
        fir.setStatus(statusAfterAssignment);
        firRepository.save(fir);

        FirUpdate caseDiary = new FirUpdate();
        caseDiary.setFir(fir);
        caseDiary.setUpdatedByUser(assignedBy);
        caseDiary.setStatusAfter(statusAfterAssignment);
        caseDiary.setRemark(firstAssignment
                ? "FIR assigned to officer ID " + officer.getId()
                : "FIR reassigned to officer ID " + officer.getId());
        caseDiary.setVisibility(UpdateVisibility.INTERNAL);
        firUpdateRepository.save(caseDiary);

        return savedAssignment;
    }

    public List<FirAssignment> getAssignmentsByOfficerId(Long officerId, User currentUser) {
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Officer not found with id: " + officerId));
        if (officer.getRole() != UserRole.OFFICER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an officer");
        }
        if (currentUser.getRole() != UserRole.ADMIN && !currentUser.getId().equals(officerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view these assignments");
        }
        return firAssignmentRepository.findByAssignedToOfficerIdAndActiveTrue(officerId);
    }
}

