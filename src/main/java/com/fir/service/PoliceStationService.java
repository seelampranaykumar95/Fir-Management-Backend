package com.fir.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fir.model.PoliceStation;
import com.fir.repository.PoliceStationRepository;

@Service
public class PoliceStationService {

    private final PoliceStationRepository policeStationRepository;

    public PoliceStationService(PoliceStationRepository policeStationRepository) {
        this.policeStationRepository = policeStationRepository;
    }

    public PoliceStation createStation(PoliceStation policeStation) {
        policeStationRepository.findByStationCode(policeStation.getStationCode()).ifPresent(existing -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Police station with code already exists: " + policeStation.getStationCode());
        });
        return policeStationRepository.save(policeStation);
    }

    public List<PoliceStation> getAllStations() {
        return policeStationRepository.findAll();
    }

    public Page<PoliceStation> getAllStations(Pageable pageable) {
        return policeStationRepository.findAll(pageable);
    }

    public PoliceStation getStationById(Long id) {
        return policeStationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Police station not found with id: " + id));
    }

    public PoliceStation updateStation(Long id, PoliceStation policeStation) {
        PoliceStation existingStation = getStationById(id);
        if (policeStationRepository.existsByStationCodeAndIdNot(policeStation.getStationCode(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Police station with code already exists: " + policeStation.getStationCode());
        }

        existingStation.setName(policeStation.getName());
        existingStation.setAddress(policeStation.getAddress());
        existingStation.setCity(policeStation.getCity());
        existingStation.setState(policeStation.getState());
        existingStation.setStationCode(policeStation.getStationCode());
        return policeStationRepository.save(existingStation);
    }

    public void deleteStation(Long id) {
        if (!policeStationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Police station not found with id: " + id);
        }
        policeStationRepository.deleteById(id);
    }
}

