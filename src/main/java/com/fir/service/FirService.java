package com.fir.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.fir.dto.CreateFirRequest;
import com.fir.dto.UpdateFirStatusRequest;
import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;
import com.fir.model.FirUpdate;
import com.fir.model.PoliceStation;
import com.fir.model.UpdateVisibility;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.FirAssignmentRepository;
import com.fir.repository.FirRepository;
import com.fir.repository.FirSpecifications;
import com.fir.repository.FirUpdateRepository;
import com.fir.repository.PoliceStationRepository;
import com.fir.repository.UserRepository;

@Service
public class FirService {

    private static final Logger log = LoggerFactory.getLogger(FirService.class);

    private final FirRepository firRepository;
    private final UserRepository userRepository;
    private final PoliceStationRepository policeStationRepository;
    private final FirUpdateRepository firUpdateRepository;
    private final FirAssignmentRepository firAssignmentRepository;
    private final FirWorkflowService firWorkflowService;

    public FirService(
            FirRepository firRepository,
            UserRepository userRepository,
            PoliceStationRepository policeStationRepository,
            FirUpdateRepository firUpdateRepository,
            FirAssignmentRepository firAssignmentRepository,
            FirWorkflowService firWorkflowService) {
        this.firRepository = firRepository;
        this.userRepository = userRepository;
        this.policeStationRepository = policeStationRepository;
        this.firUpdateRepository = firUpdateRepository;
        this.firAssignmentRepository = firAssignmentRepository;
        this.firWorkflowService = firWorkflowService;
    }

    public Fir saveFir(CreateFirRequest request) {
        User filedBy = userRepository.findById(request.getFiledByUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + request.getFiledByUserId()));
        if (filedBy.getRole() != UserRole.CITIZEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FIR can only be filed by a citizen account");
        }

        PoliceStation station = null;
        if (request.getPoliceStationId() != null) {
            station = policeStationRepository.findById(request.getPoliceStationId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Police station not found with id: " + request.getPoliceStationId()));
        }

        Fir fir = new Fir();
        fir.setTitle(request.getTitle());
        fir.setDescription(request.getDescription());
        fir.setLocation(request.getLocation());
        fir.setFiledBy(filedBy);
        fir.setPoliceStation(station);
        fir.setCategory(request.getCategory() != null ? request.getCategory() : FirCategory.OTHER);
        fir.setStatus(FirStatus.SUBMITTED);

        return firRepository.save(fir);
    }

    public Page<Fir> getAllFirs(FirStatus status, FirCategory category, Pageable pageable) {
        return firRepository.findAll(baseFilter(status, category), pageable);
    }

    public Page<Fir> getReviewQueue(FirStatus status, FirCategory category, Pageable pageable) {
        if (status != null && !firWorkflowService.isReviewStatus(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is not part of the review queue");
        }
        Specification<Fir> specification = Specification
                .where(FirSpecifications.hasReviewStatus(firWorkflowService.getReviewStatuses()))
                .and(baseFilter(status, category));
        return firRepository.findAll(specification, pageable);
    }

    public Fir getFirById(Long id) {
        return firRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FIR not found with id: " + id));
    }

    public Fir getFirByIdForUser(Long id, User currentUser) {
        Fir fir = getFirById(id);
        if (!canAccessFir(currentUser, fir)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this FIR");
        }
        return fir;
    }

    public Page<Fir> getFirsByUserId(
            Long userId,
            FirStatus status,
            FirCategory category,
            Pageable pageable,
            User authenticatedUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }
        validateUserFirAccess(userId, authenticatedUser);
        Specification<Fir> specification = Specification
                .where(FirSpecifications.filedByUser(userId))
                .and(baseFilter(status, category));
        return firRepository.findAll(specification, pageable);
    }

    public Fir updateFirStatus(Long firId, UpdateFirStatusRequest request, User updatedBy) {
        Fir fir = getFirById(firId);
        validateStatusUpdateAccess(fir, request.getStatus(), updatedBy);
        fir.setStatus(request.getStatus());
        Fir updatedFir = firRepository.save(fir);

        FirUpdate update = new FirUpdate();
        update.setFir(updatedFir);
        update.setUpdatedByUser(updatedBy);
        update.setStatusAfter(request.getStatus());
        update.setRemark(StringUtils.hasText(request.getRemark())
                ? request.getRemark()
                : "Status changed to " + request.getStatus().name());
        update.setVisibility(request.getVisibility() != null
                ? request.getVisibility()
                : UpdateVisibility.INTERNAL);
        firUpdateRepository.save(update);

        return updatedFir;
    }

    public void deleteFir(Long id) {
        if (!firRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FIR not found with id: " + id);
        }
        firRepository.deleteById(id);
    }

    private boolean canAccessFir(User currentUser, Fir fir) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == UserRole.CITIZEN) {
            return fir.getFiledBy() != null && currentUser.getId().equals(fir.getFiledBy().getId());
        }
        if (currentUser.getRole() == UserRole.OFFICER) {
            if (firWorkflowService.isReviewStatus(fir.getStatus())) {
                return true;
            }
            return firAssignmentRepository.findByFirIdAndAssignedToOfficerIdAndActiveTrue(fir.getId(), currentUser.getId())
                    .isPresent();
        }
        return false;
    }

    private void validateUserFirAccess(Long requestedUserId, User authenticatedUser) {
        log.info(
                "Authorizing FIR list access authEmail={} authUserId={} requestUserId={} role={}",
                authenticatedUser.getEmail(),
                authenticatedUser.getId(),
                requestedUserId,
                authenticatedUser.getRole());

        if (authenticatedUser.getRole() == UserRole.ADMIN) {
            return;
        }
        if (authenticatedUser.getRole() == UserRole.CITIZEN && authenticatedUser.getId().equals(requestedUserId)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own FIRs");
    }

    private void validateStatusUpdateAccess(Fir fir, FirStatus targetStatus, User updatedBy) {
        if (targetStatus == FirStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the assignment API to move an FIR to ASSIGNED");
        }
        if (updatedBy.getRole() == UserRole.ADMIN) {
            firWorkflowService.validateTransition(fir.getStatus(), targetStatus);
            return;
        }
        if (updatedBy.getRole() == UserRole.OFFICER) {
            firWorkflowService.validateOfficerReviewTransition(fir.getStatus(), targetStatus);
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update FIR status");
    }

    private Specification<Fir> baseFilter(FirStatus status, FirCategory category) {
        return Specification.where(FirSpecifications.hasStatus(status))
                .and(FirSpecifications.hasCategory(category));
    }
}

