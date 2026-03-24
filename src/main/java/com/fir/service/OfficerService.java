package com.fir.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.fir.dto.OfficerRemarkRequest;
import com.fir.dto.OfficerStatusUpdateRequest;
import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;
import com.fir.model.FirUpdate;
import com.fir.model.UpdateVisibility;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.FirAssignmentRepository;
import com.fir.repository.FirRepository;
import com.fir.repository.FirSpecifications;
import com.fir.repository.FirUpdateRepository;
import com.fir.repository.UserRepository;

@Service
public class OfficerService {

    private final UserRepository userRepository;
    private final FirRepository firRepository;
    private final FirAssignmentRepository firAssignmentRepository;
    private final FirUpdateRepository firUpdateRepository;
    private final FirWorkflowService firWorkflowService;

    public OfficerService(
            UserRepository userRepository,
            FirRepository firRepository,
            FirAssignmentRepository firAssignmentRepository,
            FirUpdateRepository firUpdateRepository,
            FirWorkflowService firWorkflowService) {
        this.userRepository = userRepository;
        this.firRepository = firRepository;
        this.firAssignmentRepository = firAssignmentRepository;
        this.firUpdateRepository = firUpdateRepository;
        this.firWorkflowService = firWorkflowService;
    }

    public Page<Fir> getAssignedFirs(Long officerId, FirStatus status, FirCategory category, Pageable pageable) {
        validateOfficer(officerId);
        Specification<Fir> specification = Specification
                .where(FirSpecifications.activelyAssignedToOfficer(officerId))
                .and(FirSpecifications.hasStatus(status))
                .and(FirSpecifications.hasCategory(category));
        return firRepository.findAll(specification, pageable);
    }

    @Transactional
    public Fir updateAssignedFirStatus(Long officerId, Long firId, OfficerStatusUpdateRequest request) {
        User officer = validateOfficer(officerId);
        firAssignmentRepository.findByFirIdAndAssignedToOfficerIdAndActiveTrue(firId, officerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "FIR is not actively assigned to this officer"));

        Fir fir = firRepository.findById(firId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FIR not found with id: " + firId));
        firWorkflowService.validateOfficerAssignedTransition(fir.getStatus(), request.getStatus());
        fir.setStatus(request.getStatus());
        Fir savedFir = firRepository.save(fir);

        FirUpdate update = new FirUpdate();
        update.setFir(savedFir);
        update.setUpdatedByUser(officer);
        update.setStatusAfter(request.getStatus());
        update.setRemark(StringUtils.hasText(request.getRemark())
                ? request.getRemark()
                : "Status updated to " + request.getStatus().name() + " by officer");
        update.setVisibility(request.getVisibility() != null ? request.getVisibility() : UpdateVisibility.INTERNAL);
        firUpdateRepository.save(update);

        return savedFir;
    }

    public FirUpdate addRemark(Long officerId, Long firId, OfficerRemarkRequest request) {
        User officer = validateOfficer(officerId);
        firAssignmentRepository.findByFirIdAndAssignedToOfficerIdAndActiveTrue(firId, officerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "FIR is not actively assigned to this officer"));

        Fir fir = firRepository.findById(firId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FIR not found with id: " + firId));

        FirUpdate update = new FirUpdate();
        update.setFir(fir);
        update.setUpdatedByUser(officer);
        update.setStatusAfter(fir.getStatus());
        update.setRemark(request.getRemark());
        update.setVisibility(request.getVisibility() != null ? request.getVisibility() : UpdateVisibility.INTERNAL);
        return firUpdateRepository.save(update);
    }

    private User validateOfficer(Long officerId) {
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Officer not found with id: " + officerId));
        if (officer.getRole() != UserRole.OFFICER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an officer");
        }
        return officer;
    }
}

