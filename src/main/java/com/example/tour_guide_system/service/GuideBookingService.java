package com.example.tour_guide_system.service;

import com.example.tour_guide_system.entity.TourPlan;
import com.example.tour_guide_system.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GuideBookingService {

    @Autowired
    private TourPlanService tourPlanService;

    public List<TourPlan> getAssignedPlans(User guide) {
        return tourPlanService.getTourPlansByGuide(guide);
    }

    public long getPendingCount(User guide) {
        return getAssignedPlans(guide).stream()
                .filter(plan -> plan.getStatus() == TourPlan.Status.PENDING_QUOTATION)
                .count();
    }

    public long getAcceptedCount(User guide) {
        return getAssignedPlans(guide).stream()
                .filter(plan -> plan.getStatus() == TourPlan.Status.ACCEPTED)
                .count();
    }

    public Optional<TourPlan> getBookingById(Long id) {
        return tourPlanService.getTourPlanById(id);
    }

    public boolean hasDuplicateDirectBooking(User tourist, User guide) {
        return tourPlanService.getTourPlansByTourist(tourist).stream()
                .anyMatch(plan -> plan.getGuide() != null
                        && plan.getGuide().getId().equals(guide.getId())
                        && (plan.getDestinations() == null || plan.getDestinations().isEmpty())
                        && plan.getStatus() != TourPlan.Status.REJECTED
                        && plan.getStatus() != TourPlan.Status.CANCELLED
                        && plan.getStatus() != TourPlan.Status.COMPLETED);
    }

    public TourPlan createDirectBooking(User tourist, User guide) {
        TourPlan booking = new TourPlan();
        booking.setTourist(tourist);
        booking.setGuide(guide);
        booking.setStatus(TourPlan.Status.PENDING_QUOTATION);
        booking.setTotalEstimatedCost(guide.getHourlyRate() != null ? guide.getHourlyRate() * 8 : 0.0);
        booking.setTouristNote(buildBookedGuideNote(guide));
        booking.setGuideNotification("New direct booking request received from " + tourist.getName() + ".");
        return tourPlanService.saveTourPlan(booking);
    }

    public void respondToQuotation(User guide, Long planId, String action) {
        TourPlan plan = tourPlanService.getTourPlanById(planId).orElse(null);
        if (plan == null || plan.getGuide() == null || !plan.getGuide().getId().equals(guide.getId())) {
            throw new IllegalArgumentException("Booking was not found.");
        }

        if (plan.getStatus() == TourPlan.Status.CANCELLED) {
            throw new IllegalArgumentException("This booking was cancelled by the tourist.");
        }

        if (plan.getStatus() != TourPlan.Status.PENDING_QUOTATION) {
            throw new IllegalArgumentException("This booking has already been processed.");
        }

        if ("accept".equalsIgnoreCase(action)) {
            plan.setStatus(TourPlan.Status.ACCEPTED);
        } else if ("reject".equalsIgnoreCase(action)) {
            plan.setStatus(TourPlan.Status.REJECTED);
        } else {
            throw new IllegalArgumentException("Please choose a valid booking action.");
        }

        tourPlanService.saveTourPlan(plan);
    }

    private String buildBookedGuideNote(User guide) {
        return "Booked guide: " + guide.getName() + ".";
    }
}
