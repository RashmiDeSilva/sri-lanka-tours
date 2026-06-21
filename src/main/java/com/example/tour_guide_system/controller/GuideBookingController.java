package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.TourPlan;
import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.service.GuideBookingService;
import com.example.tour_guide_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/home/tourguide")
public class GuideBookingController {

    @Autowired
    private UserService userService;

    @Autowired
    private GuideBookingService guideBookingService;

    @GetMapping("/{id}/tours")
    public String guideTours(@PathVariable("id") Long id, Model model, Principal principal) {
        User guide = userService.getUserById(id).orElse(null);
        if (guide == null || guide.getRole() != User.Role.GUIDE) {
            return "redirect:/login";
        }
        if (principal != null && !principal.getName().equals(guide.getEmail())) {
            return "redirect:/login";
        }

        List<TourPlan> plans = guideBookingService.getAssignedPlans(guide);
        model.addAttribute("guide", guide);
        model.addAttribute("tourPlans", plans);
        model.addAttribute("pendingCount", guideBookingService.getPendingCount(guide));
        model.addAttribute("acceptedCount", guideBookingService.getAcceptedCount(guide));
        return "guide/tours";
    }

    @PostMapping("/{id}/quotation")
    public String respondToQuotation(@PathVariable("id") Long id,
                                     @RequestParam("planId") Long planId,
                                     @RequestParam("action") String action,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        User guide = userService.getUserById(id).orElse(null);
        if (guide == null || guide.getRole() != User.Role.GUIDE) {
            redirectAttributes.addFlashAttribute("error", "Tour guide was not found.");
            return "redirect:/home/tourguide/" + id + "/tours";
        }
        if (principal == null || !principal.getName().equals(guide.getEmail())) {
            return "redirect:/login";
        }

        try {
            guideBookingService.respondToQuotation(guide, planId, action);
            redirectAttributes.addFlashAttribute("success", "Booking request updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "We could not update the booking right now.");
        }

        return "redirect:/home/tourguide/" + id + "/tours";
    }
}
