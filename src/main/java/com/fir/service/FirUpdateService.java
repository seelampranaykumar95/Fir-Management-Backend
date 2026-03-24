package com.fir.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fir.dto.CitizenInfoUpdateRequest;
import com.fir.dto.CreateFirUpdateRequest;
import com.fir.model.Fir;
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
public class FirUpdateService {

    private final FirUpdateRepository firUpdateRepository;
    private final FirRepository firRepository;
    private final UserRepository userRepository;
    private final FirAssignmentRepository firAssignmentRepository;
    private final FirWorkflowService firWorkflowService;

    public FirUpdateService(
            FirUpdateRepository firUpdateRepository,
            FirRepository firRepository,
            UserRepository userRepository,
            FirAssignmentRepository firAssignmentRepository,
            FirWorkflowService firWorkflowService) {
        this.firUpdateRepository = firUpdateRepository;
        this.firRepository = firRepository;
        this.userRepository = userRepository;
        this.firAssignmentRepository = firAssignmentRepository;
        this.firWorkflowService = firWorkflowService;
    }

    @Transactional
    public FirUpdate createFirUpdate(CreateFirUpdateRequest request, User actor) {
        Fir fir = firRepository.findById(request.getFirId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "FIR not found with id: " + request.getFirId()));
        validateUpdateAccess(fir, actor);

        FirStatus statusAfter = request.getStatusAfter() != null ? request.getStatusAfter() : fir.getStatus();
        if (request.getStatusAfter() != null) {
            validateStatusTransition(fir, actor, request.getStatusAfter());
            fir.setStatus(request.getStatusAfter());
            firRepository.save(fir);
        }

        FirUpdate update = new FirUpdate();
        update.setFir(fir);
        update.setUpdatedByUser(actor);
        update.setStatusAfter(statusAfter);
        update.setRemark(request.getRemark());
        update.setVisibility(request.getVisibility() != null ? request.getVisibility() : UpdateVisibility.INTERNAL);
        return firUpdateRepository.save(update);
    }

    @Transactional
    public FirUpdate createCitizenInfoUpdate(CitizenInfoUpdateRequest request, User actor) {
        if (actor.getRole() != UserRole.CITIZEN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only citizens can respond to NEEDS_INFO");
        }

        Fir fir = firRepository.findById(request.getFirId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "FIR not found with id: " + request.getFirId()));

        if (fir.getFiledBy() == null || !actor.getId().equals(fir.getFiledBy().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to respond to this FIR");
        }

        firWorkflowService.validateTransition(fir.getStatus(), FirStatus.PENDING_REVIEW);
        fir.setStatus(FirStatus.PENDING_REVIEW);
        firRepository.save(fir);

        FirUpdate update = new FirUpdate();
        update.setFir(fir);
        update.setUpdatedByUser(actor);
        update.setStatusAfter(FirStatus.PENDING_REVIEW);
        update.setRemark(request.getRemark());
        update.setVisibility(UpdateVisibility.PUBLIC);
        return firUpdateRepository.save(update);
    }

    public List<FirUpdate> getFirUpdatesByFirId(Long firId, User actor) {
        Fir fir = firRepository.findById(firId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FIR not found with id: " + firId));

        if (actor.getRole() == UserRole.ADMIN) {
            return firUpdateRepository.findByFirIdOrderByCreatedAtDesc(firId);
        }
        if (actor.getRole() == UserRole.OFFICER) {
            validateOfficerAssignment(firId, actor.getId());
            return firUpdateRepository.findByFirIdOrderByCreatedAtDesc(firId);
        }
        if (fir.getFiledBy() != null && actor.getId().equals(fir.getFiledBy().getId())) {
            return firUpdateRepository.findByFirIdAndVisibilityOrderByCreatedAtDesc(firId, UpdateVisibility.PUBLIC);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view updates for this FIR");
    }

    private void validateUpdateAccess(Fir fir, User actor) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.OFFICER) {
            validateOfficerAssignment(fir.getId(), actor.getId());
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to add updates to this FIR");
    }

    private void validateStatusTransition(Fir fir, User actor, FirStatus targetStatus) {
        if (targetStatus == FirStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the assignment API to move an FIR to ASSIGNED");
        }
        if (actor.getRole() == UserRole.ADMIN) {
            firWorkflowService.validateTransition(fir.getStatus(), targetStatus);
            return;
        }
        if (actor.getRole() == UserRole.OFFICER) {
            if (firAssignmentRepository.findByFirIdAndAssignedToOfficerIdAndActiveTrue(fir.getId(), actor.getId()).isPresent()) {
                firWorkflowService.validateOfficerAssignedTransition(fir.getStatus(), targetStatus);
                return;
            }
            firWorkflowService.validateOfficerReviewTransition(fir.getStatus(), targetStatus);
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to change FIR status");
    }

    private void validateOfficerAssignment(Long firId, Long officerId) {
        validateOfficer(officerId);
        firAssignmentRepository.findByFirIdAndAssignedToOfficerIdAndActiveTrue(firId, officerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "FIR is not actively assigned to this officer"));
    }

    private void validateOfficer(Long officerId) {
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Officer not found with id: " + officerId));
        if (officer.getRole() != UserRole.OFFICER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an officer");
        }
    }
}

