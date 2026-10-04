package com.erp.notif.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;

/** Unit test: with no {@link JavaMailSender} bean, an EMAIL send fails with {@code NO_MAIL_SENDER}. */
class DefaultChannelProviderTest {

    @Test
    void emailWithoutMailSender_failsWithNoMailSenderReason() {
        DefaultChannelProvider provider = new DefaultChannelProvider(
            new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class));

        ChannelSendResult result = provider.send("EMAIL", 1L, null, null,
            Map.of("email", "someone@example.com"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo(DefaultChannelProvider.NO_MAIL_SENDER);
    }

    @Test
    void nonEmailChannel_doesNotNeedAMailSender() {
        DefaultChannelProvider provider = new DefaultChannelProvider(
            new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class));

        assertThat(provider.send("SMS", 1L, null, null, Map.of()).success()).isTrue();
    }
}
