package com.example.tour_guide_system.repository;

import com.example.tour_guide_system.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByCategory(Destination.Category category);
    List<Destination> findByProvince(Destination.Province province);
    List<Destination> findByDistrict(String district);
    List<Destination> findByCity(String city);
    List<Destination> findByNameContainingIgnoreCase(String name);
}
