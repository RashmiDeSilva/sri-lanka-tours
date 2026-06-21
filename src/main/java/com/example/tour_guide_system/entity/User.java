package com.example.tour_guide_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email address is required")
    @Email(message = "Enter a valid email address")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public enum Role {
        TOURIST, GUIDE, ADMIN
    }

    private Boolean approved = true;
    private Boolean active = true;
    private Boolean blocked = false;

    // Guide Specific fields (could be a separate entity, but for simplicity we put it here)
    private Double hourlyRate;
    private String languages;
    private Double averageRating = 0.0;
    private Integer totalReviews = 0;

    // New Fields
    @Pattern(regexp = "[0-9+\\-\\s]{7,20}", message = "Please enter a valid phone number")
    private String phone;
    private String country;
    private String nic;
    
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate dob;
    
    private String gender;
    private String experience;
    private String profilePhoto;
    private String certifications;
    
    @Size(max = 1000, message = "Description must be 1000 characters or less")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Transient
    private String confirmPassword;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Boolean getApproved() { return approved; }
    public boolean isApproved() { 
        if (approvalStatus != null) {
            return approvalStatus == ApprovalStatus.APPROVED;
        }
        return approved == null || approved; 
    }
    public void setApproved(Boolean approved) { 
        this.approved = approved; 
        if (approved != null) {
            this.approvalStatus = approved ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;
        }
    }

    public Boolean getActive() { return active; }
    public boolean isActive() { return active == null || active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getBlocked() { return blocked; }
    public boolean isBlocked() { return blocked != null && blocked; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { 
        this.approvalStatus = approvalStatus; 
        this.approved = (approvalStatus == ApprovalStatus.APPROVED);
    }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public java.time.LocalDate getDob() { return dob; }
    public void setDob(java.time.LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() {
        if (dob == null) return null;
        return java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
    }

    @Transient
    public Double getEstimatedDayRate() {
        return hourlyRate == null ? 0.0 : hourlyRate * 8;
    }
}
