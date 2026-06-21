package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.repository.DestinationRepository;
import com.example.tour_guide_system.repository.ReviewRepository;
import com.example.tour_guide_system.repository.TourPlanRepository;
import com.example.tour_guide_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

@Controller
@RequestMapping("/home/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private TourPlanRepository tourPlanRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/{id}")
    public String adminHome(@PathVariable("id") Long id, Model model, Principal principal) {
        User admin = userRepository.findById(id).orElse(null);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }

        // Ensure logged-in admin is accessing their own dashboard
        if (principal != null && !principal.getName().equals(admin.getEmail())) {
            return "redirect:/login";
        }

        model.addAttribute("admin", admin);
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalDestinations", destinationRepository.count());
        model.addAttribute("totalTours", tourPlanRepository.count());
        model.addAttribute("totalReviews", reviewRepository.count());
        
        return "admin/home";
    }
}
