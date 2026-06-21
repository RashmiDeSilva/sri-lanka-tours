package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.TourPlan;
import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.entity.Review;
import com.example.tour_guide_system.service.GuideBookingService;
import com.example.tour_guide_system.service.ReviewService;
import com.example.tour_guide_system.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/home/tourguide")
public class TourGuideController {

    private static final Logger log = LoggerFactory.getLogger(TourGuideController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private GuideBookingService guideBookingService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/{id}")
    public String guideHome(@PathVariable("id") Long id, Model model, Principal principal) {
        User guide = userService.getUserById(id).orElse(null);
        if (guide == null || guide.getRole() != User.Role.GUIDE) {
            return "redirect:/login";
        }
        
        // Security check: ensure the logged-in user is accessing their own dashboard
        if (principal != null && !principal.getName().equals(guide.getEmail())) {
            return "redirect:/login";
        }

        List<TourPlan> plans = guideBookingService.getAssignedPlans(guide);
        long pendingCount = guideBookingService.getPendingCount(guide);
        long acceptedCount = guideBookingService.getAcceptedCount(guide);
        model.addAttribute("guide", guide);
        model.addAttribute("tourPlans", plans);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("acceptedCount", acceptedCount);

        List<Review> topReviews = reviewService.getRecentReviews(5);
        model.addAttribute("topReviews", topReviews);
        
        return "guide/home";
    }

    @PostMapping("/{id}/profile/update")
    public String updateProfile(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "dob", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam("hourlyRate") Double hourlyRate,
            @RequestParam("languages") String languages,
            @RequestParam("experience") String experience,
            @RequestParam("description") String description,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "profilePhotoFile", required = false) MultipartFile profilePhotoFile,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        User guide = userService.getUserById(id).orElse(null);
        if (guide == null || guide.getRole() != User.Role.GUIDE) {
            return "redirect:/login";
        }
        if (principal != null && !principal.getName().equals(guide.getEmail())) {
            return "redirect:/login";
        }

        String validationError = validateGuideProfile(name, dob, hourlyRate, languages, experience, description);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/home/tourguide/" + id;
        }

        try {
            guide.setName(name.trim());
            guide.setDob(dob);
            guide.setHourlyRate(hourlyRate);
            guide.setLanguages(languages.trim());
            guide.setExperience(experience.trim());
            guide.setDescription(description == null ? null : description.trim());
            guide.setActive(active != null && active);

            if (profilePhotoFile != null && !profilePhotoFile.isEmpty()) {
                guide.setProfilePhoto(saveProfilePhoto(profilePhotoFile));
            }

            userService.saveUser(guide);
            return "redirect:/home/tourguide/" + id + "?success=profileUpdated";
        } catch (IOException e) {
            log.error("Unable to save guide profile photo for {}", id, e);
            return "redirect:/home/tourguide/" + id + "?error=photoUploadFailed";
        } catch (Exception e) {
            log.error("Unable to update guide profile for {}", id, e);
            return "redirect:/home/tourguide/" + id + "?error=profileUpdateFailed";
        }
    }

    private String validateGuideProfile(String name,
                                        LocalDate dob,
                                        Double hourlyRate,
                                        String languages,
                                        String experience,
                                        String description) {
        if (name == null || name.trim().isEmpty()) {
            return "Full name is required.";
        }
        if (hourlyRate == null || hourlyRate <= 0) {
            return "Hourly rate must be greater than zero.";
        }
        if (languages == null || languages.trim().isEmpty()) {
            return "Please select at least one language.";
        }
        if (experience == null || experience.trim().isEmpty()) {
            return "Years of experience is required.";
        }
        if (description == null || description.trim().isEmpty()) {
            return "Description is required.";
        }
        if (dob != null && dob.isAfter(LocalDate.now())) {
            return "Date of birth cannot be in the future.";
        }
        if (description != null && description.length() > 1000) {
            return "Description is too long.";
        }
        return null;
    }

    private String saveProfilePhoto(MultipartFile profilePhotoFile) throws IOException {
        String uploadDir = "uploads/photos";
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        String originalName = profilePhotoFile.getOriginalFilename() == null ? "profile.bin" : profilePhotoFile.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalName.replaceAll("[\\\\/]+", "_").replaceAll("\\s+", "_");
        Path filePath = dir.resolve(fileName);
        Files.copy(profilePhotoFile.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/photos/" + fileName;
    }
}
