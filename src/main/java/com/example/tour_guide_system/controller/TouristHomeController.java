package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.Destination;
import com.example.tour_guide_system.entity.Review;
import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.service.DestinationService;
import com.example.tour_guide_system.service.ReviewService;
import com.example.tour_guide_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the new URL pattern: /home/tourist/{id}
 * This serves the tourist home page with the tourist's ID in the URL.
 */
@Controller
public class TouristHomeController {

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/home/tourist/{id}")
    public String touristHome(@PathVariable("id") Long id, Principal principal, Model model) {
        User tourist = userService.getUserById(id).orElse(null);
        if (tourist == null || tourist.getRole() != User.Role.TOURIST) {
            return "redirect:/login";
        }

        if (principal != null && !principal.getName().equals(tourist.getEmail())) {
            return "redirect:/login";
        }

        String touristName = tourist.getName() != null ? tourist.getName().trim() : "";
        String displayName = touristName.isEmpty() ? "Traveler" : touristName;
        String firstName = displayName.contains(" ") ? displayName.split("\\s+")[0] : displayName;

        model.addAttribute("touristId", id);
        model.addAttribute("tourist", tourist);
        model.addAttribute("touristDisplayName", displayName);
        model.addAttribute("touristFirstName", firstName);
        model.addAttribute("touristEmail", tourist.getEmail());
        model.addAttribute("destinations", destinationService.getAllDestinations());
        List<Destination> featuredDestinations = destinationService.getAllDestinations()
                .stream()
                .sorted(Comparator.comparingDouble(Destination::getAverageRating).reversed())
                .limit(4)
                .collect(Collectors.toList());
        model.addAttribute("featuredDestinations", featuredDestinations);

        List<Review> topReviews = reviewService.getRecentReviews(5);
        model.addAttribute("topReviews", topReviews);

        return "tourist/home";
    }
}
