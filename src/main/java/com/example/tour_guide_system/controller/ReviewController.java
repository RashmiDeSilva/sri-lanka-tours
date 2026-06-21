package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.Review;
import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.service.ReviewService;
import com.example.tour_guide_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @GetMapping("/reviews")
    public String viewReviews(Model model, Principal principal) {
        List<Review> reviews = reviewService.getAllReviews();
        model.addAttribute("reviews", reviews);

        boolean isAuthenticated = (principal != null);
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            User currentUser = userService.getUserByEmail(principal.getName()).orElse(null);
            model.addAttribute("currentUser", currentUser);
        }

        return "reviews"; // Resolves to templates/reviews.html
    }

    @PostMapping("/reviews/add")
    public String addReview(@RequestParam("rating") int rating,
                            @RequestParam("comment") String comment,
                            Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userService.getUserByEmail(principal.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (rating < 1 || rating > 5) {
            return "redirect:/reviews?error=invalidInput";
        }
        if (comment == null || comment.trim().isEmpty()) {
            return "redirect:/reviews?error=invalidInput";
        }

        Review review = new Review();
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setReviewerName(currentUser.getName());
        review.setAuthorEmail(currentUser.getEmail());
        review.setCreatedAt(LocalDateTime.now());

        try {
            reviewService.saveReview(review);
            return "redirect:/reviews?success=reviewAdded";
        } catch (Exception ex) {
            log.error("Unable to save review for {}", currentUser.getEmail(), ex);
            return "redirect:/reviews?error=saveFailed";
        }
    }

    @PostMapping("/reviews/{id}/edit")
    public String editReview(@PathVariable("id") Long id,
                             @RequestParam("rating") int rating,
                             @RequestParam("comment") String comment,
                             Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        Review review = reviewService.getReviewById(id).orElse(null);
        if (review == null) {
            return "redirect:/reviews?error=notFound";
        }

        if (rating < 1 || rating > 5) {
            return "redirect:/reviews?error=invalidInput";
        }
        if (comment == null || comment.trim().isEmpty()) {
            return "redirect:/reviews?error=invalidInput";
        }

        // Author or Admin check
        User currentUser = userService.getUserByEmail(principal.getName()).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == User.Role.ADMIN;
        boolean isAuthor = review.getAuthorEmail() != null && review.getAuthorEmail().equals(principal.getName());

        if (!isAuthor && !isAdmin) {
            return "redirect:/reviews?error=unauthorized";
        }

        review.setRating(rating);
        review.setComment(comment.trim());
        try {
            reviewService.saveReview(review);
            return "redirect:/reviews?success=reviewUpdated";
        } catch (Exception ex) {
            log.error("Unable to update review {}", id, ex);
            return "redirect:/reviews?error=updateFailed";
        }
    }

    @GetMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        Review review = reviewService.getReviewById(id).orElse(null);
        if (review == null) {
            return "redirect:/reviews?error=notFound";
        }

        User currentUser = userService.getUserByEmail(principal.getName()).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == User.Role.ADMIN;
        boolean isAuthor = review.getAuthorEmail() != null && review.getAuthorEmail().equals(principal.getName());

        if (!isAuthor && !isAdmin) {
            return "redirect:/reviews?error=unauthorized";
        }

        try {
            reviewService.deleteReview(review);
            return "redirect:/reviews?success=reviewDeleted";
        } catch (Exception ex) {
            log.error("Unable to delete review {}", id, ex);
            return "redirect:/reviews?error=deleteFailed";
        }
    }
}
