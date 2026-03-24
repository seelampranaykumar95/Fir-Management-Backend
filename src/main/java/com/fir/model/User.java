package com.fir.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "aadhaar_number", unique = true)
    private String aadhaarNumber;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.CITIZEN;

    @JsonIgnore
    @OneToMany(mappedBy = "filedBy")
    private List<Fir> filedFirs = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "assignedToOfficer")
    private List<FirAssignment> assignedFirEntries = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "assignedByUser")
    private List<FirAssignment> createdAssignments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "updatedByUser")
    private List<FirUpdate> firUpdates = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "uploadedByUser")
    private List<EvidenceFile> uploadedEvidenceFiles = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public User() {}

    public User(String name, String email, String password, UserRole role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role == null ? UserRole.CITIZEN : role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public List<Fir> getFiledFirs() {
        return filedFirs;
    }

    public void setFiledFirs(List<Fir> filedFirs) {
        this.filedFirs = filedFirs;
    }

    public List<FirAssignment> getAssignedFirEntries() {
        return assignedFirEntries;
    }

    public void setAssignedFirEntries(List<FirAssignment> assignedFirEntries) {
        this.assignedFirEntries = assignedFirEntries;
    }

    public List<FirAssignment> getCreatedAssignments() {
        return createdAssignments;
    }

    public void setCreatedAssignments(List<FirAssignment> createdAssignments) {
        this.createdAssignments = createdAssignments;
    }

    public List<FirUpdate> getFirUpdates() {
        return firUpdates;
    }

    public void setFirUpdates(List<FirUpdate> firUpdates) {
        this.firUpdates = firUpdates;
    }

    public List<EvidenceFile> getUploadedEvidenceFiles() {
        return uploadedEvidenceFiles;
    }

    public void setUploadedEvidenceFiles(List<EvidenceFile> uploadedEvidenceFiles) {
        this.uploadedEvidenceFiles = uploadedEvidenceFiles;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}

