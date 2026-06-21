package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.Destination;
import com.example.tour_guide_system.entity.Review;
import com.example.tour_guide_system.service.DestinationService;
import com.example.tour_guide_system.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/")
    public String home(Model model) {
        List<Destination> destinations = destinationService.getAllDestinations()
            .stream()
            .sorted(Comparator.comparingDouble(Destination::getAverageRating).reversed())
            .limit(4)
            .collect(Collectors.toList());
        model.addAttribute("destinations", destinations);

        List<Review> topReviews = reviewService.getRecentReviews(5);
        model.addAttribute("topReviews", topReviews);

        return "index";
    }

    @GetMapping("/about-us")
    public String aboutUs() {
        return "about-us";
    }

    @GetMapping("/contact-us")
    public String contactUs() {
        return "contact-us";
    }
}
