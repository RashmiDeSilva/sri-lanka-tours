package com.example.tour_guide_system.service;

import com.example.tour_guide_system.entity.TourPlan;
import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.repository.TourPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TourPlanService {

    @Autowired
    private TourPlanRepository tourPlanRepository;

    public List<TourPlan> getAllTourPlans() {
        return tourPlanRepository.findAll();
    }

    public Optional<TourPlan> getTourPlanById(Long id) {
        return tourPlanRepository.findById(id);
    }

    public List<TourPlan> getTourPlansByTourist(User tourist) {
        return tourPlanRepository.findByTouristOrderByUpdatedAtDesc(tourist);
    }

    public List<TourPlan> getTourPlansByGuide(User guide) {
        return tourPlanRepository.findByGuideOrderByUpdatedAtDesc(guide);
    }

    public List<TourPlan> getCustomTourPlansByTourist(User tourist) {
        return getTourPlansByTourist(tourist).stream()
                .filter(plan -> plan.getDestinations() != null && !plan.getDestinations().isEmpty())
                .filter(plan -> plan.getStatus() != TourPlan.Status.CANCELLED)
                .collect(Collectors.toList());
    }

    public List<TourPlan> getDirectBookingsByTourist(User tourist) {
        return getTourPlansByTourist(tourist).stream()
                .filter(plan -> plan.getDestinations() == null || plan.getDestinations().isEmpty())
                .filter(plan -> plan.getStatus() != TourPlan.Status.CANCELLED)
                .collect(Collectors.toList());
    }

    public TourPlan saveTourPlan(TourPlan tourPlan) {
        return tourPlanRepository.save(tourPlan);
    }

    public void deleteTourPlanById(Long id) {
        tourPlanRepository.deleteById(id);
    }
}
