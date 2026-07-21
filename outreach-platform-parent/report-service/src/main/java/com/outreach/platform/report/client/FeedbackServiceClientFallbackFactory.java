package com.outreach.platform.report.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * Fallback factory for FeedbackServiceClient in Report Service.
 * Returns empty/default results when feedback-service is unavailable.
 */
@Component
public class FeedbackServiceClientFallbackFactory implements FallbackFactory<FeedbackServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(FeedbackServiceClientFallbackFactory.class);

    @Override
    public FeedbackServiceClient create(Throwable cause) {
        log.warn("Feedback service unavailable, activating fallback. Cause: {}", cause.getMessage());
        return new FeedbackServiceClientFallback(cause);
    }

    private record FeedbackServiceClientFallback(Throwable cause) implements FeedbackServiceClient {

        @Override
        public FeedbackServiceClient.PagedFeedbackResponse listByEvent(UUID eventId, int page, int size) {
            log.warn("Fallback: listByEvent({}) - returning empty page. Cause: {}", eventId, cause.getMessage());
            return new FeedbackServiceClient.PagedFeedbackResponse(Collections.emptyList(), 0, 0, page, size);
        }

        @Override
        public FeedbackServiceClient.FeedbackStatusResponse getEventFeedbackStatus(UUID eventId) {
            log.warn("Fallback: getEventFeedbackStatus({}) - returning empty status. Cause: {}", eventId, cause.getMessage());
            return new FeedbackServiceClient.FeedbackStatusResponse(eventId, 0, 0, 0, 0, 0, 0.0, 0.0);
        }

        @Override
        public FeedbackServiceClient.PagedFeedbackResponse searchFeedback(UUID eventId, String category,
                                                                           Integer minScore, Integer maxScore,
                                                                           int page, int size) {
            log.warn("Fallback: searchFeedback - returning empty page. Cause: {}", cause.getMessage());
            return new FeedbackServiceClient.PagedFeedbackResponse(Collections.emptyList(), 0, 0, page, size);
        }
    }
}
