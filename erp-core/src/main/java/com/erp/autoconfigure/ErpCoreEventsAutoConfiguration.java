package com.erp.autoconfigure;

import com.erp.events.DomainEventPublisher;
import com.erp.events.ErpCoreEvents;
import com.erp.events.support.SpringDomainEventPublisher;
import com.erp.events.support.TenantAndSecurityContextTaskDecorator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The erp-core domain event bus (erp-core step 08):
 * <ul>
 *   <li>{@link DomainEventPublisher} — wraps Spring's {@link ApplicationEventPublisher}; an application
 *       may define its own bean instead;</li>
 *   <li>{@value ErpCoreEvents#EXECUTOR} — the {@link ThreadPoolTaskExecutor} asynchronous listeners name
 *       in {@code @Async(ErpCoreEvents.EXECUTOR)}. Its {@link TenantAndSecurityContextTaskDecorator}
 *       copies {@code TenantContext} and the security context into the worker thread and clears them
 *       afterwards. It is declared {@code defaultCandidate = false}: it is reachable by name only, so
 *       it neither replaces Boot's {@code applicationTaskExecutor} nor becomes the target of an
 *       application's unqualified {@code @Async} or {@code TaskExecutor} injection;</li>
 *   <li>{@code @EnableAsync}, which {@code @Async} listeners need (core does not enable scheduling).</li>
 * </ul>
 * Ordered after Boot's {@link TaskExecutionAutoConfiguration} so the application's default executor is
 * always decided first. On shutdown the executor does not wait for running tasks: a {@code QUEUED}
 * notification interrupted that way is recovered by {@code NotificationRequeueJob}.
 */
@AutoConfiguration(after = {ErpCoreAutoConfiguration.class, TaskExecutionAutoConfiguration.class})
@EnableAsync
@EnableConfigurationProperties(ErpCoreProperties.class)
public class ErpCoreEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringDomainEventPublisher(applicationEventPublisher);
    }

    @Bean(name = ErpCoreEvents.EXECUTOR, defaultCandidate = false)
    @ConditionalOnMissingBean(name = ErpCoreEvents.EXECUTOR)
    public ThreadPoolTaskExecutor erpCoreEventExecutor(ErpCoreProperties properties) {
        ErpCoreProperties.Events.Executor settings = properties.getEvents().getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(settings.getCorePoolSize());
        executor.setMaxPoolSize(Math.max(settings.getMaxPoolSize(), settings.getCorePoolSize()));
        executor.setQueueCapacity(settings.getQueueCapacity());
        executor.setThreadNamePrefix(settings.getThreadNamePrefix());
        executor.setTaskDecorator(new TenantAndSecurityContextTaskDecorator());
        return executor;
    }
}
