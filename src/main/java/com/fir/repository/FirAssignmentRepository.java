package com.fir.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fir.model.FirAssignment;

public interface FirAssignmentRepository extends JpaRepository<FirAssignment, Long> {

    List<FirAssignment> findByAssignedToOfficerId(Long officerId);

    List<FirAssignment> findByAssignedToOfficerIdAndActiveTrue(Long officerId);

    Optional<FirAssignment> findByFirIdAndActiveTrue(Long firId);

    Optional<FirAssignment> findByFirIdAndAssignedToOfficerIdAndActiveTrue(Long firId, Long officerId);
}

