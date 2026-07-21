package com.outreach.platform.feedback.service;

import com.outreach.platform.feedback.entity.VolunteerFeedbackEntity;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for exporting feedback data as CSV.
 * Uses streaming output to handle large datasets without excessive memory usage.
 */
@Service
public class FeedbackExportService {

    private static final String CSV_HEADER = "ID,Event ID,Volunteer ID,Score,Answer 1,Answer 2,Answer 3," +
            "Category,Tags,Sentiment,Status,Anonymous,Submitted At,Reviewed At,Reviewed By";

    private final FeedbackService feedbackService;

    @Inject
    public FeedbackExportService(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Writes feedback data for the given event as CSV to the provided output stream.
     */
    public void exportToCsv(UUID eventId, OutputStream outputStream) throws IOException {
        List<VolunteerFeedbackEntity> feedbackList = feedbackService.listAllByEvent(eventId);

        PrintWriter writer = new PrintWriter(outputStream, false, StandardCharsets.UTF_8);
        writer.println(CSV_HEADER);

        for (VolunteerFeedbackEntity entity : feedbackList) {
            writer.println(toCsvRow(entity));
        }

        writer.flush();
    }

    private String toCsvRow(VolunteerFeedbackEntity entity) {
        return String.join(",",
                quote(str(entity.getId())),
                quote(str(entity.getEventId())),
                quote(str(entity.getVolunteerId())),
                String.valueOf(entity.getScore()),
                quote(escapeCsv(entity.getAnswer1())),
                quote(escapeCsv(entity.getAnswer2())),
                quote(escapeCsv(entity.getAnswer3())),
                quote(escapeCsv(entity.getCategory())),
                quote(escapeCsv(entity.getTags())),
                quote(str(entity.getSentiment())),
                quote(str(entity.getStatus())),
                String.valueOf(entity.isAnonymous()),
                quote(str(entity.getSubmittedAt())),
                quote(str(entity.getReviewedAt())),
                quote(escapeCsv(entity.getReviewedBy()))
        );
    }

    private static String str(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }
}
