package com.erp.notif.channel;

import com.erp.notif.exception.NotifErrorCodes;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * The stand-in for a channel that has no real {@link ChannelProvider} bean (e.g. SMS or PUSH until an
 * application adds one, or EMAIL without a {@code JavaMailSender}): logs the message — never its
 * variables — and answers {@link DeliveryStatus#SKIPPED_NO_PROVIDER} with reason
 * {@code NOTIF_CHANNEL_UNAVAILABLE}. Never throws, never retried.
 *
 * <p>Not a bean: NOTIF's provider registry creates one per channel that no bean serves, which is how
 * "a logging provider for every channel without a real one" is realised (a bean condition cannot key
 * on the channel a bean returns).
 */
@Slf4j
public final class LoggingChannelProvider implements ChannelProvider {

    private final String channel;

    public LoggingChannelProvider(String channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public DeliveryResult send(OutboundMessage message) {
        log.info("No ChannelProvider for channel {} — {} skipped", channel, message);
        return DeliveryResult.skippedNoProvider(NotifErrorCodes.NOTIF_CHANNEL_UNAVAILABLE);
    }
}
