package com.uber.doma.domain_rider.mutual_rating_service.validator;

public record RiderRatingSubmission(
    String tripId,
    String riderId,
    String driverId,
    int stars,
    String feedbackComment
) {}
