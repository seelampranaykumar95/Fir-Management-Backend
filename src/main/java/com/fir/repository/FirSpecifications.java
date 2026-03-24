package com.fir.repository;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;

import jakarta.persistence.criteria.JoinType;

public final class FirSpecifications {

    private FirSpecifications() {
    }

    public static Specification<Fir> hasStatus(FirStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Fir> hasCategory(FirCategory category) {
        return (root, query, criteriaBuilder) ->
                category == null ? null : criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<Fir> filedByUser(Long userId) {
        return (root, query, criteriaBuilder) ->
                userId == null ? null : criteriaBuilder.equal(root.get("filedBy").get("id"), userId);
    }

    public static Specification<Fir> hasReviewStatus(Collection<FirStatus> statuses) {
        return (root, query, criteriaBuilder) -> root.get("status").in(statuses);
    }

    public static Specification<Fir> activelyAssignedToOfficer(Long officerId) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            var assignmentJoin = root.join("assignments", JoinType.INNER);
            return criteriaBuilder.and(
                    criteriaBuilder.equal(assignmentJoin.get("assignedToOfficer").get("id"), officerId),
                    criteriaBuilder.isTrue(assignmentJoin.get("active")));
        };
    }
}

