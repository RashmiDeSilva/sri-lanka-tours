package com.example.tour_guide_system.repository;

import com.example.tour_guide_system.entity.TourPlan;
import com.example.tour_guide_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourPlanRepository extends JpaRepository<TourPlan, Long> {
    List<TourPlan> findByTourist(User tourist);
    List<TourPlan> findByTouristOrderByUpdatedAtDesc(User tourist);
    List<TourPlan> findByGuide(User guide);
    List<TourPlan> findByGuideOrderByUpdatedAtDesc(User guide);
}
