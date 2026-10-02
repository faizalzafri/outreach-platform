package com.outreach.platform.notification.channel;

import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Looks up the sender for a channel name; every {@link MessageChannelSender} bean registers itself. */
@Component
public class MessageChannels {

    private final Map<String, MessageChannelSender> senders;

    @Inject
    public MessageChannels(List<MessageChannelSender> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(s -> s.channel().toUpperCase(Locale.ROOT), Function.identity()));
    }

    public MessageChannelSender sender(String channel) {
        MessageChannelSender sender = senders.get(channel == null ? "" : channel.toUpperCase(Locale.ROOT));
        if (sender == null) {
            throw new ChannelDeliveryException("No sender for channel '" + channel + "'; available: " + senders.keySet());
        }
        return sender;
    }
}
