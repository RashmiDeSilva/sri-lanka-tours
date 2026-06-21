package com.example.tour_guide_system.service;

import com.example.tour_guide_system.entity.Destination;
import com.example.tour_guide_system.repository.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DestinationService {

    @Autowired
    private DestinationRepository destinationRepository;

    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    public Optional<Destination> getDestinationById(Long id) {
        return destinationRepository.findById(id);
    }

    public List<Destination> getDestinationsByCategory(Destination.Category category) {
        return destinationRepository.findByCategory(category);
    }

    public List<Destination> getDestinationsByProvince(Destination.Province province) {
        return destinationRepository.findByProvince(province);
    }

    public List<Destination> getDestinationsByDistrict(String district) {
        return destinationRepository.findByDistrict(district);
    }

    public List<Destination> getDestinationsByCity(String city) {
        return destinationRepository.findByCity(city);
    }

    public List<Destination> searchDestinations(String name) {
        return destinationRepository.findByNameContainingIgnoreCase(name);
    }

    public Destination createDestination(Destination destination) {
        return destinationRepository.save(destination);
    }

    public Destination updateDestination(Destination destination) {
        return destinationRepository.save(destination);
    }

    public void deleteDestination(Long id) {
        destinationRepository.deleteById(id);
    }
}