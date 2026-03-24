package com.fir.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fir.model.AuditLog;
import com.fir.model.User;
import com.fir.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog recordAction(User actor, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actor == null ? null : actor.getId());
        auditLog.setActorEmail(actor == null ? "system" : actor.getEmail());
        auditLog.setActorRole(actor == null ? null : actor.getRole());
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        return auditLogRepository.save(auditLog);
    }

    public Page<AuditLog> search(
            String action,
            String entityType,
            String actorEmail,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {
        Specification<AuditLog> specification = Specification
                .where(hasAction(action))
                .and(hasEntityType(entityType))
                .and(hasActorEmail(actorEmail))
                .and(createdAtGreaterThanOrEqualTo(from))
                .and(createdAtLessThanOrEqualTo(to));
        return auditLogRepository.findAll(specification, pageable);
    }

    private Specification<AuditLog> hasAction(String action) {
        return (root, query, criteriaBuilder) -> StringUtils.hasText(action)
                ? criteriaBuilder.equal(root.get("action"), action.trim())
                : null;
    }

    private Specification<AuditLog> hasEntityType(String entityType) {
        return (root, query, criteriaBuilder) -> StringUtils.hasText(entityType)
                ? criteriaBuilder.equal(root.get("entityType"), entityType.trim())
                : null;
    }

    private Specification<AuditLog> hasActorEmail(String actorEmail) {
        return (root, query, criteriaBuilder) -> StringUtils.hasText(actorEmail)
                ? criteriaBuilder.equal(criteriaBuilder.lower(root.get("actorEmail")), actorEmail.trim().toLowerCase())
                : null;
    }

    private Specification<AuditLog> createdAtGreaterThanOrEqualTo(LocalDateTime from) {
        return (root, query, criteriaBuilder) -> from == null
                ? null
                : criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private Specification<AuditLog> createdAtLessThanOrEqualTo(LocalDateTime to) {
        return (root, query, criteriaBuilder) -> to == null
                ? null
                : criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}

