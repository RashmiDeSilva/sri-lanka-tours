package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.Destination;
import com.example.tour_guide_system.entity.TourPlan;
import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.service.DestinationService;
import com.example.tour_guide_system.service.GuideBookingService;
import com.example.tour_guide_system.service.ReviewService;
import com.example.tour_guide_system.service.TourPlanService;
import com.example.tour_guide_system.service.UserService;
import com.example.tour_guide_system.entity.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tourist")
public class TouristController {

    private static final Logger log = LoggerFactory.getLogger(TouristController.class);

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private TourPlanService tourPlanService;

    @Autowired
    private GuideBookingService guideBookingService;

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("destinations", destinationService.getAllDestinations());
        List<Destination> featuredDestinations = destinationService.getAllDestinations()
                .stream()
                .sorted(Comparator.comparingDouble(Destination::getAverageRating).reversed())
                .limit(4)
                .collect(Collectors.toList());
        model.addAttribute("featuredDestinations", featuredDestinations);
        return "tourist/home";
    }

    @GetMapping("/destinations")
    public String searchDestinations(@RequestParam(value = "search", required = false) String search,
                                     @RequestParam(value = "province", required = false) String province,
                                     @RequestParam(value = "district", required = false) String district,
                                     @RequestParam(value = "sort", required = false) String sort,
                                     Model model) {
        List<Destination> destinations = destinationService.getAllDestinations();

        // 1. Filter by search (name)
        if (search != null && !search.trim().isEmpty()) {
            destinations = destinations.stream()
                    .filter(d -> d.getName().toLowerCase().contains(search.toLowerCase().trim()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 2. Filter by province
        if (province != null && !province.trim().isEmpty()) {
            destinations = destinations.stream()
                    .filter(d -> d.getProvince().name().equalsIgnoreCase(province.trim()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 3. Filter by district
        if (district != null && !district.trim().isEmpty()) {
            destinations = destinations.stream()
                    .filter(d -> d.getDistrict().equalsIgnoreCase(district.trim()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 4. Sort
        if (sort == null || sort.isEmpty() || sort.equals("recommendation")) {
            destinations.sort(Comparator.comparingDouble(Destination::getAverageRating).reversed());
        } else if (sort.equals("price_high_low")) {
            destinations.sort(Comparator.comparingDouble(Destination::getEntryFee).reversed());
        } else if (sort.equals("price_low_high")) {
            destinations.sort(Comparator.comparingDouble(Destination::getEntryFee));
        }

        // Dynamic dropdown data
        List<String> activeDistricts = destinationService.getAllDestinations().stream()
                .map(Destination::getDistrict)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("destinations", destinations);
        model.addAttribute("provinces", Destination.Province.values());
        model.addAttribute("districts", activeDistricts);

        // Keep form state
        model.addAttribute("search", search);
        model.addAttribute("selectedProvince", province);
        model.addAttribute("selectedDistrict", district);
        model.addAttribute("selectedSort", sort);

        return "tourist/destinations";
    }

    @GetMapping("/destinations/{id}")
    public String viewDestination(@PathVariable("id") Long id, Model model) {
        Destination destination = destinationService.getDestinationById(id).orElse(null);
        model.addAttribute("destination", destination);
        if (destination != null) {
            model.addAttribute("reviews", reviewService.getReviewsByDestinationId(id));
        }
        return "tourist/destination-details";
    }

    @GetMapping("/destinations/{id}/tour-plan")
    public String createTourPlanFromDestination(@PathVariable("id") Long id) {
        if (destinationService.getDestinationById(id).isEmpty()) {
            return "redirect:/tourist/destinations";
        }
        return "redirect:/tourist/tour-plans/build?destinationId=" + id;
    }

    @PostMapping("/destinations/{id}/reviews")
    public String submitReview(@PathVariable("id") Long id, 
                               @RequestParam("reviewerName") String reviewerName, 
                               @RequestParam("rating") int rating, 
                               @RequestParam("comment") String comment,
                               Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userService.getUserByEmail(principal.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Destination destination = destinationService.getDestinationById(id).orElse(null);
        if (destination == null) {
            return "redirect:/tourist/destinations/" + id + "?error=destinationNotFound";
        }

        if (reviewerName == null || reviewerName.trim().isEmpty()
                || comment == null || comment.trim().isEmpty()
                || rating < 1 || rating > 5) {
            return "redirect:/tourist/destinations/" + id + "?error=invalidReview";
        }

        try {
            Review review = new Review();
            review.setDestination(destination);
            review.setReviewerName(reviewerName.trim());
            review.setRating(rating);
            review.setComment(comment.trim());

            review.setAuthorEmail(currentUser.getEmail());
            if (review.getReviewerName() == null || review.getReviewerName().isBlank()) {
                review.setReviewerName(currentUser.getName());
            }

            reviewService.saveReview(review);
            return "redirect:/tourist/destinations/" + id + "?success=reviewAdded";
        } catch (Exception ex) {
            log.error("Unable to save destination review for {}", id, ex);
            return "redirect:/tourist/destinations/" + id + "?error=reviewFailed";
        }
    }

    @GetMapping("/overview")
    public String overview(Model model) {
        model.addAttribute("destinations", destinationService.getAllDestinations());
        return "tourist/overview";
    }

    @GetMapping("/tour-plans")
    public String tourPlans(Model model, Principal principal) {
        if (principal != null) {
            User tourist = userService.getUserByEmail(principal.getName()).orElse(null);
            if (tourist != null) {
                model.addAttribute("tourPlans", tourPlanService.getCustomTourPlansByTourist(tourist));
            }
        }
        return "tourist/tour-plans";
    }

    @GetMapping("/tour-plans/build")
    public String buildTour(@RequestParam(value = "guideId", required = false) Long guideId,
                            @RequestParam(value = "bookingId", required = false) Long bookingId,
                            @RequestParam(value = "destinationId", required = false) Long destinationId,
                            Model model,
                            Principal principal) {
        model.addAttribute("destinations", destinationService.getAllDestinations());
        model.addAttribute("guides", userService.getUsersByRole(User.Role.GUIDE).stream()
                .filter(User::isApproved)
                .collect(Collectors.toList()));

        User tourist = principal != null ? userService.getUserByEmail(principal.getName()).orElse(null) : null;
        TourPlan existingPlan = null;
        if (bookingId != null && tourist != null) {
            existingPlan = tourPlanService.getTourPlanById(bookingId)
                    .filter(plan -> plan.getTourist() != null && plan.getTourist().getId().equals(tourist.getId()))
                    .orElse(null);
        }

        Long selectedGuideId = guideId;
        List<Long> selectedDestinationIds = new java.util.ArrayList<>();
        LocalDate startDate = null;
        LocalDate endDate = null;
        int plannedDays = 1;
        String touristNote = null;

        if (existingPlan != null) {
            if (selectedGuideId == null && existingPlan.getGuide() != null) {
                selectedGuideId = existingPlan.getGuide().getId();
            }
            if (existingPlan.getDestinations() != null) {
                selectedDestinationIds = existingPlan.getDestinations().stream()
                        .map(Destination::getId)
                        .collect(Collectors.toList());
            }
            startDate = existingPlan.getStartDate();
            endDate = existingPlan.getEndDate();
            if (startDate != null && endDate != null) {
                plannedDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
            }
            touristNote = existingPlan.getTouristNote();
            model.addAttribute("editingPlan", existingPlan);
        }

        if (destinationId != null && selectedDestinationIds.isEmpty()) {
            selectedDestinationIds.add(destinationId);
        }

        model.addAttribute("selectedGuideId", selectedGuideId);
        model.addAttribute("selectedDestinationId", destinationId);
        model.addAttribute("selectedDestinationIds", selectedDestinationIds);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("plannedDays", plannedDays);
        model.addAttribute("touristNote", touristNote);
        model.addAttribute("bookingId", bookingId);
        return "tourist/build-tour";
    }

    @PostMapping("/tour-plans/create")
    public String createTourPlan(@RequestParam(value = "destinationIds", required = false) List<Long> destinationIds,
                                 @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                 @RequestParam(value = "guideId", required = false) Long guideId,
                                 @RequestParam(value = "bookingId", required = false) Long bookingId,
                                 @RequestParam(value = "touristNote", required = false) String touristNote,
                                 @RequestParam(value = "maleAdults", required = false, defaultValue = "0") Integer maleAdults,
                                 @RequestParam(value = "femaleAdults", required = false, defaultValue = "0") Integer femaleAdults,
                                 @RequestParam(value = "boys", required = false, defaultValue = "0") Integer boys,
                                 @RequestParam(value = "girls", required = false, defaultValue = "0") Integer girls,
                                 @RequestParam(value = "seniors", required = false, defaultValue = "0") Integer seniors,
                                 @RequestParam(value = "infants", required = false, defaultValue = "0") Integer infants,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User tourist = userService.getUserByEmail(principal.getName()).orElse(null);
        if (tourist == null) return "redirect:/login";

        if (destinationIds == null || destinationIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one destination.");
            return buildTourRedirect(guideId, bookingId);
        }

        if (startDate == null || endDate == null) {
            redirectAttributes.addFlashAttribute("error", "Please choose your trip dates.");
            return buildTourRedirect(guideId, bookingId);
        }

        if (startDate.isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "The trip start date cannot be in the past.");
            return buildTourRedirect(guideId, bookingId);
        }

        if (endDate.isBefore(startDate)) {
            redirectAttributes.addFlashAttribute("error", "The end date must be the same as or after the start date.");
            return buildTourRedirect(guideId, bookingId);
        }

        int maleAdultCount = safeCount(maleAdults);
        int femaleAdultCount = safeCount(femaleAdults);
        int boyCount = safeCount(boys);
        int girlCount = safeCount(girls);
        int seniorCount = safeCount(seniors);
        int infantCount = safeCount(infants);

        int totalMembers = maleAdultCount + femaleAdultCount + boyCount + girlCount + seniorCount + infantCount;
        if (totalMembers <= 0) {
            redirectAttributes.addFlashAttribute("error", "Please enter at least one traveler.");
            return buildTourRedirect(guideId, bookingId);
        }

        TourPlan plan = null;
        boolean updatingExistingPlan = false;
        if (bookingId != null) {
            TourPlan existingPlan = tourPlanService.getTourPlanById(bookingId).orElse(null);
            if (existingPlan != null && existingPlan.getTourist() != null && existingPlan.getTourist().getId().equals(tourist.getId())) {
                if (existingPlan.getStatus() == TourPlan.Status.COMPLETED || existingPlan.getStatus() == TourPlan.Status.CANCELLED) {
                    redirectAttributes.addFlashAttribute("error", "This tour plan can no longer be edited.");
                    return "redirect:/tourist/tour-plans";
                }
                plan = existingPlan;
                updatingExistingPlan = true;
            }
        }

        if (plan == null) {
            plan = new TourPlan();
            plan.setTourist(tourist);
        }

        User selectedGuide = guideId != null ? userService.getUserById(guideId).orElse(null) : null;
        if (selectedGuide == null && plan.getGuide() != null) {
            selectedGuide = plan.getGuide();
        }
        if (selectedGuide == null || selectedGuide.getRole() != User.Role.GUIDE || !selectedGuide.isApproved()) {
            redirectAttributes.addFlashAttribute("error", "Please choose a valid tour guide.");
            return buildTourRedirect(guideId, bookingId);
        }

        plan.setGuide(selectedGuide);
        plan.setStartDate(startDate);
        plan.setEndDate(endDate);
        plan.setMaleAdults(maleAdultCount);
        plan.setFemaleAdults(femaleAdultCount);
        plan.setBoys(boyCount);
        plan.setGirls(girlCount);
        plan.setSeniors(seniorCount);
        plan.setInfants(infantCount);
        
        List<Destination> selectedDestinations = destinationIds.stream()
                .map(id -> destinationService.getDestinationById(id).orElse(null))
                .filter(d -> d != null)
                .collect(Collectors.toList());
        if (selectedDestinations.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one valid destination.");
            return buildTourRedirect(guideId, bookingId);
        }
        Set<Long> uniqueDestinationIds = new HashSet<>(destinationIds);
        if (uniqueDestinationIds.size() != destinationIds.size()) {
            redirectAttributes.addFlashAttribute("error", "Each destination can only be selected once.");
            return buildTourRedirect(guideId, bookingId);
        }
        plan.setDestinations(selectedDestinations);
        plan.setTouristNote(resolveTouristNote(touristNote, selectedGuide));
        plan.setGuideNotification(updatingExistingPlan
                ? "Tour plan updated by " + tourist.getName() + ". Please review the latest itinerary and note."
                : "New custom tour plan submitted by " + tourist.getName() + ".");

        // Simple cost calculation
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days <= 0) {
            redirectAttributes.addFlashAttribute("error", "The trip duration must be at least one day.");
            return buildTourRedirect(guideId, bookingId);
        }
        double guideCost = plan.getGuide() != null && plan.getGuide().getHourlyRate() != null ? (plan.getGuide().getHourlyRate() * 8) * days : 0;
        double destCost = selectedDestinations.stream().mapToDouble(Destination::getEntryFee).sum();

        plan.setTotalEstimatedCost(guideCost + destCost);
        plan.setStatus(TourPlan.Status.PENDING_QUOTATION);
        
        try {
            tourPlanService.saveTourPlan(plan);
            redirectAttributes.addFlashAttribute("success", updatingExistingPlan
                    ? "Your custom tour plan has been updated and shared with your guide."
                    : "Your custom tour plan has been saved successfully.");
            return "redirect:/tourist/tour-plans";
        } catch (Exception ex) {
            log.error("Unable to save custom tour plan for tourist {}", tourist.getEmail(), ex);
            redirectAttributes.addFlashAttribute("error", "We could not save your custom tour plan right now.");
            return buildTourRedirect(guideId, bookingId);
        }
    }

    @GetMapping("/bookings")
    public String bookings(Model model, Principal principal) {
        if (principal != null) {
            User tourist = userService.getUserByEmail(principal.getName()).orElse(null);
            if (tourist != null) {
                model.addAttribute("bookings", tourPlanService.getDirectBookingsByTourist(tourist));
            }
        }
        return "tourist/bookings";
    }

    @PostMapping("/bookings/{id}/delete")
    public String deleteBooking(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User tourist = getAuthenticatedTourist(principal);
        if (tourist == null) {
            return "redirect:/login";
        }

        TourPlan booking = tourPlanService.getTourPlanById(id).orElse(null);
        if (booking == null || booking.getTourist() == null || !booking.getTourist().getId().equals(tourist.getId()) || !isDirectBooking(booking)) {
            redirectAttributes.addFlashAttribute("error", "Booking was not found.");
            return "redirect:/tourist/bookings";
        }

        if (booking.getStatus() == TourPlan.Status.COMPLETED) {
            redirectAttributes.addFlashAttribute("error", "Completed bookings cannot be removed.");
            return "redirect:/tourist/bookings";
        }

        booking.setStatus(TourPlan.Status.CANCELLED);
        booking.setGuideNotification("Tourist cancelled this direct booking request.");
        tourPlanService.saveTourPlan(booking);
        redirectAttributes.addFlashAttribute("success", "Your booking request has been cancelled. The guide can still see the cancellation note.");
        return "redirect:/tourist/bookings";
    }

    @PostMapping("/tour-plans/{id}/delete")
    public String deleteTourPlan(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User tourist = getAuthenticatedTourist(principal);
        if (tourist == null) {
            return "redirect:/login";
        }

        TourPlan plan = tourPlanService.getTourPlanById(id).orElse(null);
        if (plan == null || plan.getTourist() == null || !plan.getTourist().getId().equals(tourist.getId()) || isDirectBooking(plan)) {
            redirectAttributes.addFlashAttribute("error", "Tour plan was not found.");
            return "redirect:/tourist/tour-plans";
        }

        if (plan.getStatus() == TourPlan.Status.COMPLETED) {
            redirectAttributes.addFlashAttribute("error", "Completed tour plans cannot be removed.");
            return "redirect:/tourist/tour-plans";
        }

        plan.setStatus(TourPlan.Status.CANCELLED);
        plan.setGuideNotification("Tourist cancelled this custom tour plan.");
        tourPlanService.saveTourPlan(plan);
        redirectAttributes.addFlashAttribute("success", "Your tour plan has been cancelled. The guide can still see the update.");
        return "redirect:/tourist/tour-plans";
    }

    @GetMapping("/guides")
    public String guides(@RequestParam(value = "search", required = false) String search,
                         @RequestParam(value = "language", required = false) String language,
                         @RequestParam(value = "sort", required = false) String sort,
                         Model model,
                         Principal principal) {
        List<User> guides = userService.getUsersByRole(User.Role.GUIDE).stream()
                .filter(User::isApproved)
                .collect(Collectors.toList());

        // 1. Get unique languages for the filter dropdown
        java.util.Set<String> activeLanguages = guides.stream()
                .map(User::getLanguages)
                .filter(l -> l != null && !l.trim().isEmpty())
                .flatMap(l -> java.util.Arrays.stream(l.split(",")))
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));

        // 2. Filter by search name
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            guides = guides.stream()
                    .filter(g -> g.getName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        // 3. Filter by language
        if (language != null && !language.trim().isEmpty()) {
            String targetLang = language.trim().toLowerCase();
            guides = guides.stream()
                    .filter(g -> g.getLanguages() != null && java.util.Arrays.stream(g.getLanguages().split(","))
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .anyMatch(l -> l.equals(targetLang)))
                    .collect(Collectors.toList());
        }

        // 4. Sort guides
        if (sort != null && !sort.isEmpty()) {
            if (sort.equals("rate_low_high")) {
                guides.sort((g1, g2) -> Double.compare(g1.getHourlyRate() != null ? g1.getHourlyRate() : 0.0, 
                                                       g2.getHourlyRate() != null ? g2.getHourlyRate() : 0.0));
            } else if (sort.equals("rate_high_low")) {
                guides.sort((g1, g2) -> Double.compare(g2.getHourlyRate() != null ? g2.getHourlyRate() : 0.0, 
                                                       g1.getHourlyRate() != null ? g1.getHourlyRate() : 0.0));
            } else {
                guides.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
            }
        } else {
            guides.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        }

        model.addAttribute("guides", guides);
        model.addAttribute("activeLanguages", activeLanguages);
        
        // Keep form state in model
        model.addAttribute("search", search);
        model.addAttribute("selectedLanguage", language);
        model.addAttribute("selectedSort", sort == null || sort.isEmpty() ? "name" : sort);

        // Load tourist's direct guide bookings (accepted & pending)
        if (principal != null) {
            User tourist = userService.getUserByEmail(principal.getName()).orElse(null);
            if (tourist != null) {
                List<TourPlan> directBookings = tourPlanService.getDirectBookingsByTourist(tourist);
                model.addAttribute("acceptedBookings", directBookings.stream()
                        .filter(b -> b.getStatus() == TourPlan.Status.ACCEPTED)
                        .collect(Collectors.toList()));
                model.addAttribute("pendingBookings", directBookings.stream()
                        .filter(b -> b.getStatus() == TourPlan.Status.PENDING_QUOTATION)
                        .collect(Collectors.toList()));
            }
        }

        return "tourist/guides";
    }

    @GetMapping("/guides/{id}")
    public String guideDetails(@PathVariable("id") Long id,
                               @RequestParam(value = "bookingSent", defaultValue = "false") boolean bookingSent,
                               Model model) {
        User guide = userService.getUserById(id).orElse(null);
        if (guide == null || guide.getRole() != User.Role.GUIDE || !guide.isApproved()) {
            return "redirect:/tourist/guides";
        }

        model.addAttribute("guide", guide);
        model.addAttribute("bookingSent", bookingSent);
        return "tourist/guide-details";
    }

    @PostMapping("/guides/{id}/book")
    public String bookGuide(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User tourist = userService.getUserByEmail(principal.getName()).orElse(null);
        User guide = userService.getUserById(id).orElse(null);

        if (tourist == null || tourist.getRole() != User.Role.TOURIST) {
            return "redirect:/login";
        }
        if (guide == null || guide.getRole() != User.Role.GUIDE || !guide.isApproved()) {
            return "redirect:/tourist/guides";
        }

        if (guideBookingService.hasDuplicateDirectBooking(tourist, guide)) {
            return "redirect:/tourist/guides/" + id + "?error=duplicateBooking";
        }

        try {
            guideBookingService.createDirectBooking(tourist, guide);
            return "redirect:/tourist/guides/" + id + "?bookingSent=true";
        } catch (Exception ex) {
            log.error("Unable to save booking request for guide {}", id, ex);
            return "redirect:/tourist/guides/" + id + "?error=bookingFailed";
        }
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal != null) {
            User tourist = userService.getUserByEmail(principal.getName()).orElse(null);
            model.addAttribute("tourist", tourist);
        }
        return "tourist/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User updatedUser, Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (principal != null) {
            User tourist = userService.getUserByEmail(principal.getName()).orElse(null);
            if (tourist != null) {
                String validationError = validateTouristProfile(updatedUser);
                if (validationError != null) {
                    redirectAttributes.addFlashAttribute("error", validationError);
                    return "redirect:/tourist/profile";
                }

                try {
                    tourist.setName(updatedUser.getName().trim());
                    tourist.setPhone(updatedUser.getPhone().trim());
                    tourist.setCountry(updatedUser.getCountry().trim());
                    userService.saveUser(tourist);
                    redirectAttributes.addFlashAttribute("success", "Your profile has been updated successfully.");
                } catch (Exception ex) {
                    log.error("Unable to update tourist profile for {}", tourist.getEmail(), ex);
                    redirectAttributes.addFlashAttribute("error", "We could not update your profile right now.");
                }
            }
        }
        return "redirect:/tourist/profile";
    }

    private User getAuthenticatedTourist(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.getUserByEmail(principal.getName())
                .filter(user -> user.getRole() == User.Role.TOURIST)
                .orElse(null);
    }

    private boolean isDirectBooking(TourPlan plan) {
        return plan.getDestinations() == null || plan.getDestinations().isEmpty();
    }

    private String resolveTouristNote(String touristNote, User selectedGuide) {
        if (touristNote != null && !touristNote.trim().isEmpty()) {
            return touristNote.trim();
        }
        return "Booked guide: " + selectedGuide.getName() + ".";
    }

    private String buildTourRedirect(Long guideId, Long bookingId) {
        StringBuilder redirect = new StringBuilder("redirect:/tourist/tour-plans/build");
        boolean hasQuery = false;
        if (guideId != null) {
            redirect.append(hasQuery ? "&" : "?").append("guideId=").append(guideId);
            hasQuery = true;
        }
        if (bookingId != null) {
            redirect.append(hasQuery ? "&" : "?").append("bookingId=").append(bookingId);
        }
        return redirect.toString();
    }

    private int safeCount(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private String validateTouristProfile(User updatedUser) {
        if (updatedUser.getName() == null || updatedUser.getName().trim().isEmpty()) {
            return "Full name is required.";
        }
        if (updatedUser.getPhone() == null || updatedUser.getPhone().trim().isEmpty()) {
            return "Phone number is required.";
        }
        if (!updatedUser.getPhone().trim().matches("[0-9+\\-\\s]{7,20}")) {
            return "Please enter a valid phone number.";
        }
        if (updatedUser.getCountry() == null || updatedUser.getCountry().trim().isEmpty()) {
            return "Country is required.";
        }
        return null;
    }
}
