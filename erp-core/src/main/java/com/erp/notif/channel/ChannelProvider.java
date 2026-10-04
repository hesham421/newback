package com.erp.notif.channel;

/**
 * The NOTIF channel SPI (erp-core step 08): one implementation delivers messages over one channel.
 *
 * <p>Core ships {@link EmailChannelProvider} (registered when a {@code JavaMailSender} bean exists) and
 * {@link InAppChannelProvider} (the {@code NOTIF_INBOX}). A channel with no provider bean is served by
 * {@link LoggingChannelProvider}, which logs and answers {@link DeliveryStatus#SKIPPED_NO_PROVIDER}.
 * An application adds a channel — or replaces a core provider — by defining a bean, e.g.:
 * <pre>{@code
 * @Bean ChannelProvider smsChannelProvider(MySmsGateway gateway) {
 *     return new ChannelProvider() {
 *         public String channel() { return NotifChannels.SMS; }
 *         public DeliveryResult send(OutboundMessage m) { ... return DeliveryResult.sent(); }
 *     };
 * }
 * }</pre>
 * An application provider for a channel always wins over the core one.
 *
 * <p><b>Threading and errors.</b> {@link #send} runs on the core event executor with
 * {@code TenantContext} set to the message's tenant, outside any transaction (no database connection
 * is held while a provider talks to a remote system; a provider that writes opens its own). Return
 * {@link DeliveryResult#failed} (or throw) for a failure worth retrying — NOTIF retries with
 * exponential backoff and marks the {@code NOTIF_LOG} row {@code FAILED} once the attempts are
 * exhausted.
 */
public interface ChannelProvider {

    /** The channel code this provider delivers, e.g. {@value NotifChannels#EMAIL} (see {@link NotifChannels}). */
    String channel();

    /** Delivers one message. */
    DeliveryResult send(OutboundMessage message);
}
