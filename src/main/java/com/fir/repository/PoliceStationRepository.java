package com.fir.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fir.model.PoliceStation;

public interface PoliceStationRepository extends JpaRepository<PoliceStation, Long> {

    Optional<PoliceStation> findByStationCode(String stationCode);

    boolean existsByStationCodeAndIdNot(String stationCode, Long id);
}

