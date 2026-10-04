package com.erp.notif.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit test (erp-core step 08, replaces step 02's {@code DefaultChannelProviderTest}, whose class was
 * deleted with the synchronous send path): the core EMAIL provider renders and sends through
 * {@link JavaMailSender}, and reports a send failure or a missing address as {@code FAILED} instead of
 * throwing. The "no mail sender" case is now "no EmailChannelProvider bean" — see
 * {@code ErpCoreNotifAutoConfigurationTest} and {@code ChannelProviderRegistryTest}.
 */
class EmailChannelProviderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailChannelProvider provider = new EmailChannelProvider(mailSender, "noreply@example.test");

    @Test
    void sendsARenderedEmail_andReportsSent() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        DeliveryResult result = provider.send(message(Map.of("email", "to@example.test", "name", "Dana")));

        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo("Hello Dana");
        assertThat(sent.getValue().getAllRecipients()[0].toString()).isEqualTo("to@example.test");
    }

    @Test
    void aSendFailure_isReportedAsFailed_notThrown() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("connection refused")).when(mailSender).send(any(MimeMessage.class));

        DeliveryResult result = provider.send(message(Map.of("email", "to@example.test")));

        assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(result.detail()).contains("connection refused");
    }

    @Test
    void aMissingAddress_isFailed_withoutTouchingTheMailSender() {
        DeliveryResult result = provider.send(message(Map.of()));

        assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(result.detail()).isEqualTo(EmailChannelProvider.MISSING_EMAIL);
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(provider.channel()).isEqualTo(NotifChannels.EMAIL);
    }

    private static OutboundMessage message(Map<String, String> variables) {
        return new OutboundMessage(1L, 1L, NotifChannels.EMAIL, 2L, "T", "اسم", "Name", "مرحبا {name}",
            "Hello {name}", "نص {name}", "Body {name}", "TEST", null, null, variables);
    }
}
