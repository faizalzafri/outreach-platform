package com.outreach.platform.notification.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.notification.channel.EmailChannelSender;
import com.outreach.platform.notification.channel.MessageChannels;
import com.outreach.platform.notification.channel.OutboundMessage;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Emails a scheduled report, produced by report-service, to the schedule's recipients. */
@Service
public class ScheduledReportMailer {

    public static final String EVENT_TYPE = "ScheduledReportReady";

    private final MessageChannels channels;

    @Inject
    public ScheduledReportMailer(MessageChannels channels) {
        this.channels = channels;
    }

    @SuppressWarnings("unchecked")
    public void send(DomainEventMessage message) {
        Map<String, Object> payload = message.payload();
        String name = String.valueOf(payload.get("name"));
        OutboundMessage.Attachment file = new OutboundMessage.Attachment(String.valueOf(payload.get("fileName")),
                Base64.getDecoder().decode(String.valueOf(payload.get("content"))));
        String text = "Your scheduled report \"" + name + "\" is attached.";
        String html = "<p>Your scheduled report <strong>" + HtmlUtils.htmlEscape(name) + "</strong> is attached.</p>";
        for (String recipient : (List<String>) payload.getOrDefault("recipients", List.of())) {
            channels.sender(EmailChannelSender.CHANNEL)
                    .send(new OutboundMessage(recipient, recipient, "Scheduled report: " + name, html, text, file));
        }
    }
}
