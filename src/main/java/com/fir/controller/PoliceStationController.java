package com.fir.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fir.dto.PageResponse;
import com.fir.dto.PoliceStationDtos;
import com.fir.dto.PoliceStationDtos.CreatePoliceStationRequest;
import com.fir.dto.PoliceStationDtos.PoliceStationResponse;
import com.fir.model.PoliceStation;
import com.fir.service.AuditLogService;
import com.fir.service.PoliceStationService;
import com.fir.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/stations")
@PreAuthorize("hasRole('ADMIN')")
public class PoliceStationController {

    private final PoliceStationService policeStationService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public PoliceStationController(
            PoliceStationService policeStationService,
            AuditLogService auditLogService,
            UserService userService) {
        this.policeStationService = policeStationService;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<PoliceStationResponse> createStation(
            @Valid @RequestBody CreatePoliceStationRequest request,
            Authentication authentication) {
        PoliceStation toSave = PoliceStationDtos.toEntity(request);
        PoliceStation saved = policeStationService.createStation(toSave);
        auditLogService.recordAction(
                userService.getByEmail(authentication.getName()),
                "POLICE_STATION_CREATED",
                "POLICE_STATION",
                saved.getId(),
                "Created station " + saved.getStationCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(PoliceStationDtos.fromEntity(saved));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PoliceStationResponse>> getAllStations(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                policeStationService.getAllStations(pageable).map(PoliceStationDtos::fromEntity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoliceStationResponse> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(PoliceStationDtos.fromEntity(policeStationService.getStationById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PoliceStationResponse> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody CreatePoliceStationRequest request,
            Authentication authentication) {
        PoliceStation toUpdate = PoliceStationDtos.toEntity(request);
        PoliceStation updated = policeStationService.updateStation(id, toUpdate);
        auditLogService.recordAction(
                userService.getByEmail(authentication.getName()),
                "POLICE_STATION_UPDATED",
                "POLICE_STATION",
                updated.getId(),
                "Updated station " + updated.getStationCode());
        return ResponseEntity.ok(PoliceStationDtos.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id, Authentication authentication) {
        policeStationService.deleteStation(id);
        auditLogService.recordAction(
                userService.getByEmail(authentication.getName()),
                "POLICE_STATION_DELETED",
                "POLICE_STATION",
                id,
                "Deleted station");
        return ResponseEntity.noContent().build();
    }
}

