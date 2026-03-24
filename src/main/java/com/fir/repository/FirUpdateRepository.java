package com.fir.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fir.model.FirUpdate;
import com.fir.model.UpdateVisibility;

public interface FirUpdateRepository extends JpaRepository<FirUpdate, Long> {

    List<FirUpdate> findByFirIdOrderByCreatedAtDesc(Long firId);

    List<FirUpdate> findByFirIdAndVisibilityOrderByCreatedAtDesc(Long firId, UpdateVisibility visibility);
}

