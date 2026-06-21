package com.example.tour_guide_system.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "destinations")
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must be 1000 characters or less")
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Category is required")
    private Category category;

    @Min(value = 0, message = "Entry fee cannot be negative")
    private double entryFee = 0.00;

    @Min(value = 1, message = "Recommendation score must be at least 1")
    @Max(value = 5, message = "Recommendation score cannot exceed 5")
    private int recommendationScore = 1;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Province is required")
    private Province province;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "City is required")
    private String city;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    @NotNull(message = "Opening hours are required")
    @Column(columnDefinition = "TIME")
    private LocalTime openingHours = LocalTime.of(0, 0); // Default to 00:00

    @NotNull(message = "Closing hours are required")
    @Column(columnDefinition = "TIME")
    private LocalTime closingHours = LocalTime.of(0, 0); // Default to 00:00

    @Column(length = 500)
    @Size(max = 500, message = "Best time to visit must be 500 characters or less")
    private String bestTimeToVisit;

    private String status = "Active"; // Default to Active


    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Review> reviews;

    public enum Category {
        NATURE, ADVENTURE, CULTURE
    }

    public enum Province {
        WESTERN("Western"),
        CENTRAL("Central"),
        SOUTHERN("Southern"),
        NORTHERN("Northern"),
        EASTERN("Eastern"),
        NORTH_WESTERN("North Western"),
        NORTH_CENTRAL("North Central"),
        UVA("Uva"),
        SABARAGAMUWA("Sabaragamuwa");

        private final String displayName;

        Province(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final Map<Province, List<String>> VALID_DISTRICTS = Map.of(
            Province.WESTERN, Arrays.asList("Colombo", "Gampaha", "Kalutara"),
            Province.CENTRAL, Arrays.asList("Kandy", "Matale", "Nuwara Eliya"),
            Province.SOUTHERN, Arrays.asList("Galle", "Matara", "Hambantota"),
            Province.NORTHERN, Arrays.asList( "Jaffna", "Kilinochchi", "Mannar", "Vavuniya", "Mullaitivu"),
            Province.EASTERN, Arrays.asList("Trincomalee", "Batticaloa", "Ampara"),
            Province.NORTH_WESTERN, Arrays.asList("Kurunegala", "Puttalam"),
            Province.NORTH_CENTRAL, Arrays.asList("Anuradhapura", "Polonnaruwa"),
            Province.UVA, Arrays.asList("Badulla", "Moneragala"),
            Province.SABARAGAMUWA, Arrays.asList("Ratnapura", "Kegalle")
    );

    public Destination() {}

    public Destination(String name, String description, Category category, double entryFee, int recommendationScore,
                       Province province, String district, String city, LocalTime openingHours, LocalTime closingHours, String bestTimeToVisit) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.entryFee = entryFee;
        this.recommendationScore = recommendationScore;
        this.province = province;
        this.district = district;
        this.city = city;
        this.setOpeningHours(openingHours); // Use setter for validation
        this.setClosingHours(closingHours); // Use setter for validation
        this.bestTimeToVisit = bestTimeToVisit;
        validateDistrict();
    }

    @PrePersist
    private void validateDistrict() {
        validateTime();
        if (province != null && district != null) {
            List<String> validDistricts = VALID_DISTRICTS.get(province);
            if (validDistricts == null || !validDistricts.contains(district)) {
                throw new IllegalArgumentException("Invalid district for province " + province + ": " + district);
            }
        }
    }

    private void validateTime() {
        if (openingHours == null || closingHours == null) {
            throw new IllegalArgumentException("Opening and closing hours cannot be null");
        }
        if (openingHours.isAfter(closingHours)) {
            throw new IllegalArgumentException("Opening hours must be before closing hours");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public double getEntryFee() { return entryFee; }
    public void setEntryFee(double entryFee) { this.entryFee = entryFee; }
    public int getRecommendationScore() { return recommendationScore; }
    public void setRecommendationScore(int recommendationScore) {
        if (recommendationScore < 1 || recommendationScore > 5) {
            throw new IllegalArgumentException("Recommendation score must be between 1 and 5");
        }
        this.recommendationScore = recommendationScore;
    }
    public Province getProvince() { return province; }
    public String getFormattedProvince() {
        return province != null ? province.getDisplayName() : "";
    }
    public void setProvince(Province province) {
        this.province = province;
    }
    public String getDistrict() { return district; }
    public void setDistrict(String district) {
        this.district = district;
    }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalTime getOpeningHours() { return openingHours; }
    public void setOpeningHours(LocalTime openingHours) {
        if (openingHours == null) {
            this.openingHours = LocalTime.of(0, 0);
        } else if (openingHours.getHour() > 23 || openingHours.getMinute() > 59) {
            throw new IllegalArgumentException("Opening hours must be between 00:00 and 23:59");
        } else {
            this.openingHours = openingHours;
        }
    }
    public LocalTime getClosingHours() { return closingHours; }
    public void setClosingHours(LocalTime closingHours) {
        if (closingHours == null) {
            this.closingHours = LocalTime.of(0, 0);
        } else if (closingHours.getHour() > 23 || closingHours.getMinute() > 59) {
            throw new IllegalArgumentException("Closing hours must be between 00:00 and 23:59");
        } else {
            this.closingHours = closingHours;
        }
    }
    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }


    @PreUpdate
    public void preUpdate() {
        validateDistrict();
        this.updatedAt = LocalDateTime.now();
        validateTime();
    }

    public double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Review review : reviews) {
            sum += review.getRating();
        }
        return sum / reviews.size();
    }

    public int getAverageRatingRounded() {
        double avg = getAverageRating();
        if (avg == 0.0) {
            return 0;
        }
        return (int) Math.round(avg);
    }
}
