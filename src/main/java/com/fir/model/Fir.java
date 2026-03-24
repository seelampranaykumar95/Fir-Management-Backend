package com.fir.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "firs")
public class Fir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FirStatus status = FirStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private FirCategory category = FirCategory.OTHER;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "filed_by_user_id")
    private User filedBy;

    @ManyToOne
    @JoinColumn(name = "police_station_id")
    private PoliceStation policeStation;

    @JsonIgnore
    @OneToMany(mappedBy = "fir")
    private List<FirAssignment> assignments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "fir")
    private List<FirUpdate> updates = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "fir")
    private List<EvidenceFile> evidenceFiles = new ArrayList<>();

    public Fir() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = FirStatus.SUBMITTED;
        }
        if (category == null) {
            category = FirCategory.OTHER;
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public FirStatus getStatus() {
        return status;
    }

    public void setStatus(FirStatus status) {
        this.status = status;
    }

    public FirCategory getCategory() {
        return category;
    }

    public void setCategory(FirCategory category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getFiledBy() {
        return filedBy;
    }

    public void setFiledBy(User filedBy) {
        this.filedBy = filedBy;
    }

    public PoliceStation getPoliceStation() {
        return policeStation;
    }

    public void setPoliceStation(PoliceStation policeStation) {
        this.policeStation = policeStation;
    }

    public List<FirAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<FirAssignment> assignments) {
        this.assignments = assignments;
    }

    public List<FirUpdate> getUpdates() {
        return updates;
    }

    public void setUpdates(List<FirUpdate> updates) {
        this.updates = updates;
    }

    public List<EvidenceFile> getEvidenceFiles() {
        return evidenceFiles;
    }

    public void setEvidenceFiles(List<EvidenceFile> evidenceFiles) {
        this.evidenceFiles = evidenceFiles;
    }
}

