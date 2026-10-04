package com.erp.notif.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.erp.notif.channel.ChannelProvider;
import com.erp.notif.channel.DeliveryResult;
import com.erp.notif.channel.DeliveryStatus;
import com.erp.notif.channel.EmailChannelProvider;
import com.erp.notif.channel.LoggingChannelProvider;
import com.erp.notif.channel.NotifChannels;
import com.erp.notif.channel.OutboundMessage;
import com.erp.notif.exception.NotifErrorCodes;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit test (erp-core step 08): channel → provider resolution. An application provider beats the core
 * one, the core one serves otherwise, and a channel without any provider gets the logging stand-in,
 * which answers SKIPPED_NO_PROVIDER without throwing.
 */
class ChannelProviderRegistryTest {

    @Test
    void channelWithoutAProvider_getsTheLoggingProvider_whichSkipsWithoutThrowing() {
        ChannelProviderRegistry registry = registry(Map.of());

        ChannelProvider sms = registry.resolve(NotifChannels.SMS);
        assertThat(sms).isInstanceOf(LoggingChannelProvider.class);
        assertThat(sms.channel()).isEqualTo(NotifChannels.SMS);

        DeliveryResult result = sms.send(message(NotifChannels.SMS));
        assertThat(result.status()).isEqualTo(DeliveryStatus.SKIPPED_NO_PROVIDER);
        assertThat(result.detail()).isEqualTo(NotifErrorCodes.NOTIF_CHANNEL_UNAVAILABLE);

        // EMAIL without a JavaMailSender (no EmailChannelProvider bean) is skipped the same way
        assertThat(registry.resolve(NotifChannels.EMAIL)).isInstanceOf(LoggingChannelProvider.class);
    }

    @Test
    void theCoreProviderServesItsChannel_butAnApplicationProviderReplacesIt() {
        EmailChannelProvider core = new EmailChannelProvider(mock(JavaMailSender.class), "");
        assertThat(registry(Map.of("emailChannelProvider", core)).resolve(NotifChannels.EMAIL)).isSameAs(core);

        ChannelProvider appEmail = new ChannelProvider() {
            @Override
            public String channel() {
                return NotifChannels.EMAIL;
            }

            @Override
            public DeliveryResult send(OutboundMessage message) {
                return DeliveryResult.sent();
            }
        };
        assertThat(registry(Map.of("emailChannelProvider", core, "appEmail", appEmail))
            .resolve(NotifChannels.EMAIL)).isSameAs(appEmail);
    }

    private static ChannelProviderRegistry registry(Map<String, ChannelProvider> beans) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        beans.forEach(factory::addBean);
        return new ChannelProviderRegistry(factory.getBeanProvider(ChannelProvider.class));
    }

    private static OutboundMessage message(String channel) {
        return new OutboundMessage(1L, 1L, channel, 2L, "T", "a", "b", null, null, "c", "d", "TEST", null, null,
            Map.of("phone", "+1"));
    }
}
