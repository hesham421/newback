package com.erp.notif.channel;

/**
 * Channel codes of the NOTIF channel SPI ({@code NOTIF_CHANNEL} lookup, {@code NOTIF_CHANNEL_CONFIG}).
 * EMAIL and IN_APP have core providers; SMS and PUSH are contracts an application implements.
 */
public final class NotifChannels {

    private NotifChannels() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static final String EMAIL = "EMAIL";
    public static final String SMS = "SMS";
    public static final String PUSH = "PUSH";
    public static final String IN_APP = "IN_APP";
}
