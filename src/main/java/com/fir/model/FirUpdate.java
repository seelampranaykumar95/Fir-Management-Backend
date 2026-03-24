package com.fir.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "fir_updates")
public class FirUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fir_id", nullable = false)
    private Fir fir;

    @ManyToOne(optional = false)
    @JoinColumn(name = "updated_by_user_id", nullable = false)
    private User updatedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after")
    private FirStatus statusAfter;

    @Column(nullable = false, length = 1000)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UpdateVisibility visibility = UpdateVisibility.INTERNAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FirUpdate() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (visibility == null) {
            visibility = UpdateVisibility.INTERNAL;
        }
    }

    public Long getId() {
        return id;
    }

    public Fir getFir() {
        return fir;
    }

    public void setFir(Fir fir) {
        this.fir = fir;
    }

    public User getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(User updatedByUser) {
        this.updatedByUser = updatedByUser;
    }

    public FirStatus getStatusAfter() {
        return statusAfter;
    }

    public void setStatusAfter(FirStatus statusAfter) {
        this.statusAfter = statusAfter;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public UpdateVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(UpdateVisibility visibility) {
        this.visibility = visibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

