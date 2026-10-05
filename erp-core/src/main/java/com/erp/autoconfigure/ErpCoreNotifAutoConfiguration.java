package com.erp.autoconfigure;

import com.erp.events.DomainEventPublisher;
import com.erp.notif.channel.EmailChannelProvider;
import com.erp.notif.repository.NotificationLogRepository;
import com.erp.notif.service.NotificationDeliveryTracker;
import com.erp.notif.service.NotificationRequeueJob;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.EnableRetry;

/**
 * NOTIF's asynchronous delivery wiring (erp-core step 08):
 * <ul>
 *   <li>{@code @EnableRetry} — Spring Retry for the delivery worker's {@code @Retryable};</li>
 *   <li>{@link EmailChannelProvider} — only when a {@code JavaMailSender} bean exists (Spring Mail on
 *       the classpath and {@code spring.mail.host} set); without one EMAIL is served by the logging
 *       provider and ends {@code SKIPPED_NO_PROVIDER}. An application's own EMAIL provider bean wins;</li>
 *   <li>{@link NotificationRequeueJob} — only with {@code erp.core.notif.requeue.enabled=true}.</li>
 * </ul>
 * Ordered after Boot's mail auto-configuration (by name: Spring Mail is optional) so its
 * {@code JavaMailSender} is known when the EMAIL condition is evaluated. A component-scanned
 * {@code @ConditionalOnBean} would be evaluated before any auto-configured bean exists.
 */
@AutoConfiguration(after = ErpCoreAutoConfiguration.class,
    afterName = "org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration")
@EnableRetry
@EnableConfigurationProperties(ErpCoreProperties.class)
public class ErpCoreNotifAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "erp.core.notif.requeue", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public NotificationRequeueJob notificationRequeueJob(NotificationLogRepository logRepository,
                                                         DomainEventPublisher eventPublisher,
                                                         JdbcTemplate jdbcTemplate,
                                                         ErpCoreProperties properties,
                                                         ObjectProvider<NotificationDeliveryTracker> tracker) {
        // the delivery listener's tracker; a fresh one only when the notif components are not scanned
        return new NotificationRequeueJob(logRepository, eventPublisher, jdbcTemplate, properties,
            tracker.getIfAvailable(NotificationDeliveryTracker::new));
    }

    /** Spring Mail is optional: everything that names its classes lives behind this class check. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.mail.javamail.JavaMailSender")
    static class EmailChannelConfiguration {

        @Bean
        @ConditionalOnBean(type = "org.springframework.mail.javamail.JavaMailSender")
        @ConditionalOnMissingBean(EmailChannelProvider.class)
        EmailChannelProvider emailChannelProvider(JavaMailSender mailSender, Environment environment) {
            return new EmailChannelProvider(mailSender, environment.getProperty("spring.mail.username", ""));
        }
    }
}
