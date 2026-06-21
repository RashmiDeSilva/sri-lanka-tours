package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.Destination;
import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.entity.TourPlan;
import com.example.tour_guide_system.entity.Review;
import com.example.tour_guide_system.repository.UserRepository;
import com.example.tour_guide_system.repository.TourPlanRepository;
import com.example.tour_guide_system.repository.ReviewRepository;
import com.example.tour_guide_system.service.DestinationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminPanelController {

    private static final Logger log = LoggerFactory.getLogger(AdminPanelController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private TourPlanRepository tourPlanRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @ModelAttribute("currentAdmin")
    public User currentAdmin(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/defaultLoginRedirect";
    }

    @GetMapping("/users")
    public String users(@RequestParam(value = "role", required = false, defaultValue = "ALL") String role,
                        @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                        Model model) {
        List<User> allUsers = userRepository.findAll();
        List<User> guides = userRepository.findByRole(User.Role.GUIDE);
        List<User> users = allUsers.stream()
                .filter(user -> role.equals("ALL") || user.getRole().name().equals(role))
                .filter(user -> matchesStatus(user, status))
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalAdmins", userRepository.findByRole(User.Role.ADMIN).size());
        model.addAttribute("totalTourists", userRepository.findByRole(User.Role.TOURIST).size());
        model.addAttribute("totalGuides", guides.size());
        model.addAttribute("activeUsers", allUsers.stream().filter(user -> user.isActive() && !user.isBlocked()
                && (user.getRole() != User.Role.GUIDE || user.isApproved())).count());
        model.addAttribute("deactiveUsers", allUsers.stream().filter(user -> !user.isActive()).count());
        model.addAttribute("blockedUsers", allUsers.stream().filter(User::isBlocked).count());
        model.addAttribute("pendingGuides", guides.stream()
                .filter(guide -> !guide.isApproved())
                .collect(Collectors.toList()));
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("filteredUsers", users.size());
        model.addAttribute("roles", User.Role.values());

        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User account was not found.");
            return "redirect:/admin/users";
        }

        model.addAttribute("user", user);
        return "admin/user-details";
    }

    @PostMapping("/users/{id}/approve-guide")
    public String approveGuide(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null || user.getRole() != User.Role.GUIDE) {
            redirectAttributes.addFlashAttribute("error", "Tour guide account was not found.");
            return "redirect:/admin/users";
        }

        user.setApproved(true);
        user.setApprovalStatus(User.ApprovalStatus.APPROVED);
        user.setActive(true);
        user.setBlocked(false);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", user.getName() + " has been approved as a tour guide.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reject-guide")
    public String rejectGuide(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null || user.getRole() != User.Role.GUIDE) {
            redirectAttributes.addFlashAttribute("error", "Tour guide account was not found.");
            return "redirect:/admin/users";
        }

        user.setApproved(false);
        user.setApprovalStatus(User.ApprovalStatus.REJECTED);
        user.setActive(false); // Rejected guides cannot log in
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", user.getName() + " has been rejected as a tour guide.");
        return "redirect:/admin/users";
    }

    @GetMapping("/create-admin")
    public String showCreateAdminForm(Model model) {
        model.addAttribute("adminForm", new User());
        return "admin/create-admin";
    }

    @PostMapping("/create-admin")
    public String createAdmin(@ModelAttribute("adminForm") User newAdmin,
                              @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        String validationError = validateAdminCreation(newAdmin, confirmPassword);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/admin/create-admin";
        }

        String email = newAdmin.getEmail() != null ? newAdmin.getEmail().trim() : "";
        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email address is already in use.");
            return "redirect:/admin/create-admin";
        }

        try {
            newAdmin.setName(newAdmin.getName().trim());
            newAdmin.setEmail(email);
            newAdmin.setPhone(newAdmin.getPhone().trim());
            newAdmin.setPassword(passwordEncoder.encode(newAdmin.getPassword()));
            newAdmin.setRole(User.Role.ADMIN);
            newAdmin.setApproved(true);
            newAdmin.setApprovalStatus(User.ApprovalStatus.APPROVED);
            newAdmin.setActive(true);
            newAdmin.setBlocked(false);
            userRepository.save(newAdmin);

            redirectAttributes.addFlashAttribute("success", "Admin account created successfully for " + newAdmin.getName());
            return "redirect:/admin/users";
        } catch (DataIntegrityViolationException ex) {
            log.warn("Admin account creation failed for {}", email, ex);
            redirectAttributes.addFlashAttribute("error", "This email address is already in use.");
            return "redirect:/admin/create-admin";
        } catch (Exception ex) {
            log.error("Unexpected admin creation failure for {}", email, ex);
            redirectAttributes.addFlashAttribute("error", "Admin account could not be created right now.");
            return "redirect:/admin/create-admin";
        }
    }

    @PostMapping("/users/{id}/block")
    public String blockUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User account was not found.");
            return "redirect:/admin/users";
        }

        user.setBlocked(true);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", user.getName() + " has been blocked and cannot log in.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/unblock")
    public String unblockUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User account was not found.");
            return "redirect:/admin/users";
        }

        user.setBlocked(false);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", user.getName() + " has been unblocked.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User account was not found.");
            return "redirect:/admin/users";
        }

        user.setActive(false);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", user.getName() + " has been deactivated.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User account was not found.");
            return "redirect:/admin/users";
        }

        user.setActive(true);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", user.getName() + " has been activated.");
        return "redirect:/admin/users/" + id;
    }

    @GetMapping("/destinations")
    public String destinations(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "province", required = false) Destination.Province province,
                               @RequestParam(value = "district", required = false) String district,
                               @RequestParam(value = "category", required = false) Destination.Category category,
                               Model model) {
        List<Destination> allDestinations = destinationService.getAllDestinations();
        List<Destination> destinations = allDestinations.stream()
                .filter(destination -> search == null || search.isBlank()
                        || containsIgnoreCase(destination.getName(), search)
                        || containsIgnoreCase(destination.getCity(), search)
                        || containsIgnoreCase(destination.getDistrict(), search))
                .filter(destination -> province == null || destination.getProvince() == province)
                .filter(destination -> district == null || district.isBlank() || destination.getDistrict().equals(district))
                .filter(destination -> category == null || destination.getCategory() == category)
                .collect(Collectors.toList());

        model.addAttribute("destinations", destinations);
        model.addAttribute("totalDestinations", allDestinations.size());
        model.addAttribute("filteredDestinations", destinations.size());
        model.addAttribute("provinces", Destination.Province.values());
        model.addAttribute("categories", Arrays.asList(
                Destination.Category.CULTURE,
                Destination.Category.NATURE,
                Destination.Category.ADVENTURE
        ));
        model.addAttribute("districts", allDestinations.stream()
                .map(Destination::getDistrict)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));
        model.addAttribute("search", search);
        model.addAttribute("selectedProvince", province != null ? province.name() : "");
        model.addAttribute("selectedDistrict", district);
        model.addAttribute("selectedCategory", category != null ? category.name() : "");
        return "admin/destinations";
    }

    @GetMapping("/destinations/new")
    public String newDestination(Model model) {
        model.addAttribute("destination", new Destination());
        addDestinationOptions(model);
        return "admin/add-destination";
    }

    @PostMapping("/destinations")
    public String createDestination(@Valid @ModelAttribute("destination") Destination destination,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addDestinationOptions(model);
            return "admin/add-destination";
        }

        destinationService.createDestination(destination);
        redirectAttributes.addFlashAttribute("success", "Destination added successfully.");
        return "redirect:/admin/destinations";
    }

    @GetMapping("/destinations/{id}/edit")
    public String editDestination(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Destination destination = destinationService.getDestinationById(id).orElse(null);
        if (destination == null) {
            redirectAttributes.addFlashAttribute("error", "Destination was not found.");
            return "redirect:/admin/destinations";
        }

        model.addAttribute("destination", destination);
        addDestinationOptions(model);
        return "admin/edit-destination";
    }

    @PostMapping("/destinations/{id}")
    public String updateDestination(@PathVariable("id") Long id,
                                    @Valid @ModelAttribute("destination") Destination destination,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            destination.setId(id);
            addDestinationOptions(model);
            return "admin/edit-destination";
        }

        Destination existingDestination = destinationService.getDestinationById(id).orElse(null);
        if (existingDestination == null) {
            redirectAttributes.addFlashAttribute("error", "Destination was not found.");
            return "redirect:/admin/destinations";
        }

        existingDestination.setName(destination.getName());
        existingDestination.setDescription(destination.getDescription());
        existingDestination.setCategory(destination.getCategory());
        existingDestination.setEntryFee(destination.getEntryFee());
        existingDestination.setRecommendationScore(destination.getRecommendationScore());
        existingDestination.setProvince(destination.getProvince());
        existingDestination.setDistrict(destination.getDistrict());
        existingDestination.setCity(destination.getCity());
        existingDestination.setImageUrl(destination.getImageUrl());
        existingDestination.setOpeningHours(destination.getOpeningHours());
        existingDestination.setClosingHours(destination.getClosingHours());
        existingDestination.setBestTimeToVisit(destination.getBestTimeToVisit());
        existingDestination.setStatus(destination.getStatus());

        destinationService.updateDestination(existingDestination);
        redirectAttributes.addFlashAttribute("success", "Destination updated successfully.");
        return "redirect:/admin/destinations";
    }

    @GetMapping("/destinations/{id}/delete")
    public String deleteDestination(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (destinationService.getDestinationById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Destination was not found.");
            return "redirect:/admin/destinations";
        }

        destinationService.deleteDestination(id);
        redirectAttributes.addFlashAttribute("success", "Destination deleted successfully.");
        return "redirect:/admin/destinations";
    }

    @GetMapping("/guides")
    public String manageGuides(Model model) {
        List<User> guides = userRepository.findByRole(User.Role.GUIDE);
        model.addAttribute("guides", guides);
        return "admin/guides";
    }



    @GetMapping("/guides/{id}/delete")
    public String deleteGuide(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User guide = userRepository.findById(id).orElse(null);
        if (guide == null || guide.getRole() != User.Role.GUIDE) {
            redirectAttributes.addFlashAttribute("error", "Tour guide was not found.");
            return "redirect:/admin/guides";
        }
        userRepository.delete(guide);
        redirectAttributes.addFlashAttribute("success", "Tour Guide deleted successfully.");
        return "redirect:/admin/guides";
    }

    @GetMapping("/guides/{id}/edit")
    public String editGuideForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        User guide = userRepository.findById(id).orElse(null);
        if (guide == null || guide.getRole() != User.Role.GUIDE) {
            redirectAttributes.addFlashAttribute("error", "Tour guide was not found.");
            return "redirect:/admin/guides";
        }
        model.addAttribute("guide", guide);
        return "admin/edit-guide";
    }

    @PostMapping("/guides/{id}")
    public String updateGuide(@PathVariable("id") Long id, @ModelAttribute("guide") User guideData, RedirectAttributes redirectAttributes) {
        User existingGuide = userRepository.findById(id).orElse(null);
        if (existingGuide == null || existingGuide.getRole() != User.Role.GUIDE) {
            redirectAttributes.addFlashAttribute("error", "Tour guide was not found.");
            return "redirect:/admin/guides";
        }

        String validationError = validateGuideUpdate(guideData);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/admin/guides/" + id + "/edit";
        }

        // Email uniqueness check (exclude the current guide's email)
        if (!existingGuide.getEmail().equalsIgnoreCase(guideData.getEmail())) {
            if (userRepository.findByEmail(guideData.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email is already in use.");
                return "redirect:/admin/guides/" + id + "/edit";
            }
        }

        try {
            existingGuide.setName(guideData.getName().trim());
            existingGuide.setEmail(guideData.getEmail().trim());
            existingGuide.setPhone(guideData.getPhone().trim());
            existingGuide.setHourlyRate(guideData.getHourlyRate());
            existingGuide.setLanguages(guideData.getLanguages() != null ? guideData.getLanguages().trim() : null);
            existingGuide.setExperience(guideData.getExperience() != null ? guideData.getExperience().trim() : null);
            existingGuide.setNic(guideData.getNic() != null ? guideData.getNic().trim() : null);
            existingGuide.setDob(guideData.getDob());
            existingGuide.setGender(guideData.getGender());
            existingGuide.setDescription(guideData.getDescription());

            userRepository.save(existingGuide);
            redirectAttributes.addFlashAttribute("success", "Tour Guide updated successfully.");
            return "redirect:/admin/guides";
        } catch (DataIntegrityViolationException ex) {
            log.warn("Guide update failed for {}", id, ex);
            redirectAttributes.addFlashAttribute("error", "This email address is already in use.");
            return "redirect:/admin/guides/" + id + "/edit";
        } catch (Exception ex) {
            log.error("Unexpected guide update failure for {}", id, ex);
            redirectAttributes.addFlashAttribute("error", "Tour guide could not be updated right now.");
            return "redirect:/admin/guides/" + id + "/edit";
        }
    }

    private void addDestinationOptions(Model model) {
        model.addAttribute("categories", Destination.Category.values());
        model.addAttribute("provinces", Destination.Province.values());
    }

    private boolean matchesStatus(User user, String status) {
        return switch (status) {
            case "ACTIVE" -> user.isActive() && !user.isBlocked() && (user.getRole() != User.Role.GUIDE || user.isApproved());
            case "DEACTIVE" -> !user.isActive();
            case "PENDING" -> user.getRole() == User.Role.GUIDE && user.getApprovalStatus() == User.ApprovalStatus.PENDING;
            case "REJECTED" -> user.getRole() == User.Role.GUIDE && user.getApprovalStatus() == User.ApprovalStatus.REJECTED;
            case "BLOCKED" -> user.isBlocked();
            default -> true;
        };
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase().contains(search.toLowerCase());
    }

    @GetMapping("/bookings")
    public String bookings(Model model) {
        List<TourPlan> allPlans = tourPlanRepository.findAll();
        List<TourPlan> directBookings = allPlans.stream()
                .filter(plan -> plan.getDestinations() == null || plan.getDestinations().isEmpty())
                .collect(Collectors.toList());
        model.addAttribute("bookings", directBookings);
        model.addAttribute("totalBookings", directBookings.size());
        model.addAttribute("pendingBookings", directBookings.stream().filter(b -> b.getStatus() == TourPlan.Status.PENDING_QUOTATION).count());
        model.addAttribute("acceptedBookings", directBookings.stream().filter(b -> b.getStatus() == TourPlan.Status.ACCEPTED).count());
        model.addAttribute("rejectedBookings", directBookings.stream().filter(b -> b.getStatus() == TourPlan.Status.REJECTED).count());
        model.addAttribute("cancelledBookings", directBookings.stream().filter(b -> b.getStatus() == TourPlan.Status.CANCELLED).count());
        return "admin/bookings";
    }

    @PostMapping("/bookings/{id}/delete")
    public String deleteBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        tourPlanRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Booking deleted successfully.");
        return "redirect:/admin/bookings";
    }

    @GetMapping("/tour-plans")
    public String tourPlans(Model model) {
        List<TourPlan> allPlans = tourPlanRepository.findAll();
        List<TourPlan> customPlans = allPlans.stream()
                .filter(plan -> plan.getDestinations() != null && !plan.getDestinations().isEmpty())
                .collect(Collectors.toList());
        model.addAttribute("tourPlans", customPlans);
        model.addAttribute("totalTours", customPlans.size());
        model.addAttribute("pendingTours", customPlans.stream().filter(t -> t.getStatus() == TourPlan.Status.PENDING_QUOTATION).count());
        model.addAttribute("acceptedTours", customPlans.stream().filter(t -> t.getStatus() == TourPlan.Status.ACCEPTED).count());
        model.addAttribute("rejectedTours", customPlans.stream().filter(t -> t.getStatus() == TourPlan.Status.REJECTED).count());
        model.addAttribute("cancelledTours", customPlans.stream().filter(t -> t.getStatus() == TourPlan.Status.CANCELLED).count());
        return "admin/tour-plans";
    }

    @PostMapping("/tour-plans/{id}/delete")
    public String deleteTourPlan(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        tourPlanRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Tour plan deleted successfully.");
        return "redirect:/admin/tour-plans";
    }

    @GetMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        userRepository.delete(user);
        redirectAttributes.addFlashAttribute("success", "User " + user.getName() + " deleted successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        return "admin/edit-user";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable("id") Long id, @ModelAttribute("user") User userData, RedirectAttributes redirectAttributes) {
        User existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }

        String validationError = validateUserUpdate(userData);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/admin/users/" + id + "/edit";
        }

        if (!existingUser.getEmail().equalsIgnoreCase(userData.getEmail())) {
            if (userRepository.findByEmail(userData.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email is already in use.");
                return "redirect:/admin/users/" + id + "/edit";
            }
        }

        try {
            existingUser.setName(userData.getName().trim());
            existingUser.setEmail(userData.getEmail().trim());
            existingUser.setPhone(userData.getPhone() != null && !userData.getPhone().trim().isEmpty() ? userData.getPhone().trim() : null);
            existingUser.setCountry(userData.getCountry() != null && !userData.getCountry().trim().isEmpty() ? userData.getCountry().trim() : null);
            existingUser.setActive(userData.getActive());
            existingUser.setBlocked(userData.getBlocked());

            userRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("success", "User details updated successfully.");
            return "redirect:/admin/users";
        } catch (DataIntegrityViolationException ex) {
            log.warn("User update failed for {}", id, ex);
            redirectAttributes.addFlashAttribute("error", "This email address is already in use.");
            return "redirect:/admin/users/" + id + "/edit";
        } catch (Exception ex) {
            log.error("Unexpected user update failure for {}", id, ex);
            redirectAttributes.addFlashAttribute("error", "User details could not be updated right now.");
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @GetMapping("/tour-plans/{id}")
    public String tourPlanDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        TourPlan plan = tourPlanRepository.findById(id).orElse(null);
        if (plan == null) {
            redirectAttributes.addFlashAttribute("error", "Tour plan not found.");
            return "redirect:/admin/tour-plans";
        }
        model.addAttribute("plan", plan);
        return "admin/tour-plan-details";
    }

    @GetMapping("/tour-plans/{id}/edit")
    public String editTourPlanForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        TourPlan plan = tourPlanRepository.findById(id).orElse(null);
        if (plan == null) {
            redirectAttributes.addFlashAttribute("error", "Tour plan not found.");
            return "redirect:/admin/tour-plans";
        }
        model.addAttribute("plan", plan);
        model.addAttribute("guides", userRepository.findByRole(User.Role.GUIDE));
        model.addAttribute("statuses", TourPlan.Status.values());
        return "admin/edit-tour-plan";
    }

    @PostMapping("/tour-plans/{id}")
    public String updateTourPlan(@PathVariable("id") Long id,
                                 @RequestParam("status") TourPlan.Status status,
                                 @RequestParam(value = "guideId", required = false) Long guideId,
                                 @RequestParam("totalEstimatedCost") Double totalEstimatedCost,
                                 @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                                 @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                                 RedirectAttributes redirectAttributes) {
        TourPlan plan = tourPlanRepository.findById(id).orElse(null);
        if (plan == null) {
            redirectAttributes.addFlashAttribute("error", "Tour plan not found.");
            return "redirect:/admin/tour-plans";
        }

        if (totalEstimatedCost == null || totalEstimatedCost < 0) {
            redirectAttributes.addFlashAttribute("error", "Estimated cost must be zero or greater.");
            return plan.getDestinations() == null || plan.getDestinations().isEmpty()
                    ? "redirect:/admin/bookings"
                    : "redirect:/admin/tour-plans/" + id + "/edit";
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            redirectAttributes.addFlashAttribute("error", "End date must be the same as or after the start date.");
            return plan.getDestinations() == null || plan.getDestinations().isEmpty()
                    ? "redirect:/admin/bookings"
                    : "redirect:/admin/tour-plans/" + id + "/edit";
        }

        User selectedGuide = null;
        if (guideId != null) {
            selectedGuide = userRepository.findById(guideId).orElse(null);
            if (selectedGuide == null || selectedGuide.getRole() != User.Role.GUIDE) {
                redirectAttributes.addFlashAttribute("error", "Please choose a valid tour guide.");
                return plan.getDestinations() == null || plan.getDestinations().isEmpty()
                        ? "redirect:/admin/bookings"
                        : "redirect:/admin/tour-plans/" + id + "/edit";
            }
        }

        try {
            plan.setStatus(status);
            plan.setTotalEstimatedCost(totalEstimatedCost);
            plan.setStartDate(startDate);
            plan.setEndDate(endDate);
            plan.setGuide(selectedGuide);

            tourPlanRepository.save(plan);
            redirectAttributes.addFlashAttribute("success", "Tour plan updated successfully.");

            if (plan.getDestinations() == null || plan.getDestinations().isEmpty()) {
                return "redirect:/admin/bookings";
            }
            return "redirect:/admin/tour-plans";
        } catch (Exception ex) {
            log.error("Unexpected tour plan update failure for {}", id, ex);
            redirectAttributes.addFlashAttribute("error", "Tour plan could not be updated right now.");
            return plan.getDestinations() == null || plan.getDestinations().isEmpty()
                    ? "redirect:/admin/bookings"
                    : "redirect:/admin/tour-plans/" + id + "/edit";
        }
    }

    @GetMapping("/bookings/{id}/delete")
    public String deleteBookingGet(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (tourPlanRepository.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Booking was not found.");
            return "redirect:/admin/bookings";
        }
        tourPlanRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Booking deleted successfully.");
        return "redirect:/admin/bookings";
    }

    @GetMapping("/tour-plans/{id}/delete")
    public String deleteTourPlanGet(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (tourPlanRepository.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tour plan was not found.");
            return "redirect:/admin/tour-plans";
        }
        tourPlanRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Tour plan deleted successfully.");
        return "redirect:/admin/tour-plans";
    }

    @GetMapping("/reviews")
    public String manageReviews(Model model) {
        List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewAverage", reviews.isEmpty()
                ? null
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0.0));
        model.addAttribute("fiveStarReviewCount", reviews.stream().filter(review -> review.getRating() == 5).count());
        return "admin/reviews";
    }

    @GetMapping("/reviews/{id}/delete")
    public String deleteReviewAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (reviewRepository.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Review was not found.");
            return "redirect:/admin/reviews";
        }
        reviewRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Review deleted successfully.");
        return "redirect:/admin/reviews";
    }

    private String validateAdminCreation(User newAdmin, String confirmPassword) {
        if (newAdmin.getName() == null || newAdmin.getName().trim().isEmpty()) {
            return "Full name is required.";
        }
        if (newAdmin.getEmail() == null || newAdmin.getEmail().trim().isEmpty()) {
            return "Email address is required.";
        }
        if (!newAdmin.getEmail().trim().contains("@")) {
            return "Please enter a valid email address.";
        }
        if (newAdmin.getPhone() == null || newAdmin.getPhone().trim().isEmpty()) {
            return "Phone number is required.";
        }
        if (!newAdmin.getPhone().trim().matches("[0-9+\\-\\s]{7,20}")) {
            return "Please enter a valid phone number.";
        }
        if (newAdmin.getPassword() == null || newAdmin.getPassword().length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (confirmPassword == null || !newAdmin.getPassword().equals(confirmPassword)) {
            return "Passwords do not match.";
        }
        return null;
    }

    private String validateUserUpdate(User userData) {
        if (userData.getName() == null || userData.getName().trim().isEmpty()) {
            return "Full name is required.";
        }
        if (userData.getEmail() == null || userData.getEmail().trim().isEmpty()) {
            return "Email address is required.";
        }
        if (!userData.getEmail().trim().contains("@")) {
            return "Please enter a valid email address.";
        }
        if (userData.getPhone() != null && !userData.getPhone().trim().isEmpty()
                && !userData.getPhone().trim().matches("[0-9+\\-\\s]{7,20}")) {
            return "Please enter a valid phone number.";
        }
        return null;
    }

    private String validateGuideUpdate(User guideData) {
        if (guideData.getName() == null || guideData.getName().trim().isEmpty()) {
            return "Full name is required.";
        }
        if (guideData.getEmail() == null || guideData.getEmail().trim().isEmpty()) {
            return "Email address is required.";
        }
        if (!guideData.getEmail().trim().contains("@")) {
            return "Please enter a valid email address.";
        }
        if (guideData.getPhone() == null || guideData.getPhone().trim().isEmpty()) {
            return "Phone number is required.";
        }
        if (!guideData.getPhone().trim().matches("[0-9+\\-\\s]{7,20}")) {
            return "Please enter a valid phone number.";
        }
        if (guideData.getHourlyRate() == null || guideData.getHourlyRate() <= 0) {
            return "Hourly rate must be greater than zero.";
        }
        if (guideData.getLanguages() == null || guideData.getLanguages().trim().isEmpty()) {
            return "Please select at least one language.";
        }
        if (guideData.getExperience() == null || guideData.getExperience().trim().isEmpty()) {
            return "Years of experience is required.";
        }
        if (guideData.getDescription() == null || guideData.getDescription().trim().isEmpty()) {
            return "Description is required.";
        }
        if (guideData.getDescription().length() > 1000) {
            return "Description is too long.";
        }
        if (guideData.getNic() == null || guideData.getNic().trim().isEmpty()) {
            return "NIC number is required.";
        }
        if (guideData.getDob() != null && guideData.getDob().isAfter(LocalDate.now())) {
            return "Date of birth cannot be in the future.";
        }
        return null;
    }
}
