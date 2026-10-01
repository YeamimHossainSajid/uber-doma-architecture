package com.uber.doma.domain_rider.mutual_rating_service.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RatingBoundaryValidator {
    private static final Logger log = LoggerFactory.getLogger(RatingBoundaryValidator.class);

    public void validateRating(RiderRatingSubmission submission) {
        if (submission.stars() < 1 || submission.stars() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }

        if (submission.feedbackComment() != null && submission.feedbackComment().length() > 500) {
            throw new IllegalArgumentException("Feedback comment exceeds max allowed length of 500 characters");
        }

        log.info("[MutualRating] Validated {} star rating for trip {}", submission.stars(), submission.tripId());
    }
}
