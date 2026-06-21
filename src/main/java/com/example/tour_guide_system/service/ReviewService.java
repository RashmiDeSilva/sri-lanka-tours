package com.example.tour_guide_system.service;

import com.example.tour_guide_system.entity.Review;
import com.example.tour_guide_system.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Review> getReviewsByDestinationId(Long destinationId) {
        return reviewRepository.findByDestinationIdOrderByCreatedAtDesc(destinationId);
    }

    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public Review saveReview(Review review) {
        return reviewRepository.save(review);
    }

    public void deleteReview(Review review) {
        reviewRepository.delete(review);
    }

    public void deleteReviewById(Long id) {
        reviewRepository.deleteById(id);
    }

    public List<Review> getRecentReviews(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return getAllReviews().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
