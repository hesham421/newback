package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.erp.events.DomainEvent;
import com.erp.events.DomainEventPublisher;
import com.erp.events.ErpCoreEvents;
import com.erp.events.support.SpringDomainEventPublisher;
import com.erp.events.support.TenantAndSecurityContextTaskDecorator;
import com.erp.notif.channel.EmailChannelProvider;
import com.erp.notif.repository.NotificationLogRepository;
import com.erp.notif.service.NotificationRequeueJob;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

/** erp-core step 08 — what the events and NOTIF auto-configurations contribute, and when they back off. */
class ErpCoreEventsAndNotifAutoConfigurationTest {

    private static final String[] REQUIRED_SECRETS = {
        "erp.core.security.jwt.secret=test-only-jwt-secret-0123456789abcdef0123456789abcdef",
        "erp.core.files.access-token-secret=test-only-file-secret-0123456789abcdef0123456789"};

    private final ApplicationContextRunner events = new ApplicationContextRunner()
        .withPropertyValues(REQUIRED_SECRETS)
        .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
            ErpCoreEventsAutoConfiguration.class));

    private final ApplicationContextRunner notif = new ApplicationContextRunner()
        .withPropertyValues(REQUIRED_SECRETS)
        .withConfiguration(AutoConfigurations.of(ErpCoreNotifAutoConfiguration.class))
        .withBean(NotificationLogRepository.class, () -> mock(NotificationLogRepository.class))
        .withBean(DomainEventPublisher.class, () -> mock(DomainEventPublisher.class))
        .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void eventBus_publisherAndANamedOnlyExecutor_thatLeavesBootsApplicationTaskExecutorInCharge() {
        events.run(context -> {
            assertThat(context).hasSingleBean(DomainEventPublisher.class);
            assertThat(context.getBean(DomainEventPublisher.class)).isInstanceOf(SpringDomainEventPublisher.class);

            ThreadPoolTaskExecutor executor = context.getBean(ErpCoreEvents.EXECUTOR, ThreadPoolTaskExecutor.class);
            assertThat(executor.getThreadNamePrefix()).isEqualTo("erp-event-");
            assertThat(ReflectionTestUtils.getField(executor, "taskDecorator"))
                .isInstanceOf(TenantAndSecurityContextTaskDecorator.class);

            // Boot's default executor still exists and is what an unqualified injection gets
            assertThat(context).hasBean("applicationTaskExecutor");
            assertThat(context.getBeanProvider(TaskExecutor.class).getIfUnique())
                .isSameAs(context.getBean("applicationTaskExecutor"));
        });
    }

    @Test
    void eventBus_anApplicationPublisherReplacesTheCoreOne() {
        DomainEventPublisher own = (DomainEvent event) -> { };
        events.withBean(DomainEventPublisher.class, () -> own).run(context -> {
            assertThat(context).hasSingleBean(DomainEventPublisher.class);
            assertThat(context.getBean(DomainEventPublisher.class)).isSameAs(own);
        });
    }

    @Test
    void notif_requeueJobOnlyWhenEnabled_emailProviderOnlyWithAMailSender() {
        notif.run(context -> {
            assertThat(context).doesNotHaveBean(NotificationRequeueJob.class);
            assertThat(context).doesNotHaveBean(EmailChannelProvider.class);
        });
        notif.withPropertyValues("erp.core.notif.requeue.enabled=true")
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .run(context -> {
                assertThat(context).hasSingleBean(NotificationRequeueJob.class);
                assertThat(context).hasSingleBean(EmailChannelProvider.class);
            });
    }
}
