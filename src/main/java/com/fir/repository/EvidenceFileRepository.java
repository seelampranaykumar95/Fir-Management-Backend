package com.fir.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fir.model.EvidenceFile;

public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, Long> {

    List<EvidenceFile> findByFirIdOrderByCreatedAtDesc(Long firId);
}

