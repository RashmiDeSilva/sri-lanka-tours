package com.example.tour_guide_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tour_plans")
public class TourPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Tourist is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tourist_id", nullable = false)
    private User tourist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = true)
    private User guide;

    @NotNull(message = "Plan status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING_QUOTATION;

    @Size(max = 1000, message = "Tourist note must be 1000 characters or less")
    @Column(columnDefinition = "TEXT")
    private String touristNote;

    @Size(max = 1000, message = "Guide notification must be 1000 characters or less")
    @Column(columnDefinition = "TEXT")
    private String guideNotification;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime updatedAt;

    private Double totalEstimatedCost;

    private Integer maleAdults = 0;
    private Integer femaleAdults = 0;
    private Integer boys = 0;
    private Integer girls = 0;
    private Integer seniors = 0;
    private Integer infants = 0;

    @ManyToMany
    @JoinTable(
        name = "tour_plan_destinations",
        joinColumns = @JoinColumn(name = "tour_plan_id"),
        inverseJoinColumns = @JoinColumn(name = "destination_id")
    )
    private List<Destination> destinations = new ArrayList<>();

    public enum Status {
        PENDING_QUOTATION, ACCEPTED, REJECTED, COMPLETED, CANCELLED
    }

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTourist() { return tourist; }
    public void setTourist(User tourist) { this.tourist = tourist; }

    public User getGuide() { return guide; }
    public void setGuide(User guide) { this.guide = guide; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getTouristNote() { return touristNote; }
    public void setTouristNote(String touristNote) { this.touristNote = touristNote; }

    public String getGuideNotification() { return guideNotification; }
    public void setGuideNotification(String guideNotification) { this.guideNotification = guideNotification; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Double getTotalEstimatedCost() { return totalEstimatedCost; }
    public void setTotalEstimatedCost(Double totalEstimatedCost) { this.totalEstimatedCost = totalEstimatedCost; }

    public Integer getMaleAdults() { return maleAdults; }
    public void setMaleAdults(Integer maleAdults) { this.maleAdults = maleAdults; }

    public Integer getFemaleAdults() { return femaleAdults; }
    public void setFemaleAdults(Integer femaleAdults) { this.femaleAdults = femaleAdults; }

    public Integer getBoys() { return boys; }
    public void setBoys(Integer boys) { this.boys = boys; }

    public Integer getGirls() { return girls; }
    public void setGirls(Integer girls) { this.girls = girls; }

    public Integer getSeniors() { return seniors; }
    public void setSeniors(Integer seniors) { this.seniors = seniors; }

    public Integer getInfants() { return infants; }
    public void setInfants(Integer infants) { this.infants = infants; }

    public List<Destination> getDestinations() { return destinations; }
    public void setDestinations(List<Destination> destinations) { this.destinations = destinations; }

    @Transient
    public int getAdultMembers() {
        return safeCount(maleAdults) + safeCount(femaleAdults);
    }

    @Transient
    public int getChildMembers() {
        return safeCount(boys) + safeCount(girls);
    }

    @Transient
    public int getOptionalMembers() {
        return safeCount(seniors) + safeCount(infants);
    }

    @Transient
    public int getTotalMembers() {
        return getAdultMembers() + getChildMembers() + getOptionalMembers();
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }
}
