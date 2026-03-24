package com.fir.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.fir.model.Fir;
import com.fir.model.FirStatus;

public interface FirRepository extends JpaRepository<Fir, Long>, JpaSpecificationExecutor<Fir> {

    List<Fir> findByFiledById(Long userId);

    List<Fir> findByPoliceStationId(Long policeStationId);

    List<Fir> findByStatusInOrderByCreatedAtDesc(Collection<FirStatus> statuses);

    @Query("SELECT f.status, COUNT(f) FROM Fir f GROUP BY f.status")
    List<Object[]> fetchStatusCounts();

    @Query("SELECT f.category, COUNT(f) FROM Fir f GROUP BY f.category")
    List<Object[]> fetchCategoryCounts();

    @Query("SELECT year(f.createdAt), month(f.createdAt), COUNT(f) " +
            "FROM Fir f WHERE f.createdAt IS NOT NULL " +
            "GROUP BY year(f.createdAt), month(f.createdAt) " +
            "ORDER BY year(f.createdAt), month(f.createdAt)")
    List<Object[]> fetchMonthlyCounts();
}

