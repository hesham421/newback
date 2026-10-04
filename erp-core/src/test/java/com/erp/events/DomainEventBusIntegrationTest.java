package com.erp.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import com.erp.testsupport.DomainEventProbe;
import com.erp.testsupport.ProbeEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * erp-core step 08 — the domain event bus end to end, through {@link DomainEventPublisher} and an
 * {@code @Async(ErpCoreEvents.EXECUTOR) @TransactionalEventListener(AFTER_COMMIT, fallbackExecution)}
 * listener ({@link DomainEventProbe}): delivery after commit only, nothing on rollback, immediate
 * delivery outside a transaction, and the publisher's tenant and principal on the worker thread.
 */
class DomainEventBusIntegrationTest extends AbstractAsyncIntegrationTest {

    @Autowired
    private DomainEventPublisher publisher;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private List<TaskExecutor> defaultTaskExecutors;
    @Value("${local.server.port}")
    private int port;

    @Test
    void eventPublishedInATransaction_isDeliveredOnlyAfterCommit_andNeverOnRollback() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            publisher.publish(new ProbeEvent("rolled-back"));
            status.setRollbackOnly();
        });

        tx.executeWithoutResult(status -> {
            publisher.publish(new ProbeEvent("committed"));
            // still inside the transaction: an AFTER_COMMIT listener has not been invoked yet
            assertThat(probe.find(ProbeEvent.class, e -> e.getMarker().equals("committed"))).isEmpty();
        });

        DomainEventProbe.Received committed = await().atMost(ASYNC_TIMEOUT)
            .until(() -> probe.find(ProbeEvent.class, e -> e.getMarker().equals("committed")).orElse(null),
                r -> r != null);
        assertThat(committed.threadName()).startsWith("erp-event-");
        assertThat(committed.transactionActive()).isFalse();

        // The rolled-back event was discarded at completion of its transaction and never handed to
        // the executor; once the executor is idle its absence is final.
        awaitExecutorIdle();
        assertThat(probe.find(ProbeEvent.class, e -> e.getMarker().equals("rolled-back"))).isEmpty();
    }

    @Test
    void eventPublishedOutsideATransaction_isDeliveredImmediately_withTheTenantAndPrincipalOfThePublisher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "event-tester", null, List.of(new SimpleGrantedAuthority("ANY"))));

        TenantContext.runAs(777L, () -> publisher.publish(new ProbeEvent("tenant-777")));

        DomainEventProbe.Received received = await().atMost(ASYNC_TIMEOUT)
            .until(() -> probe.find(ProbeEvent.class, e -> e.getMarker().equals("tenant-777")).orElse(null),
                r -> r != null);
        assertThat(received.event().getTenantId()).isEqualTo(777L);
        assertThat(received.tenantInThread()).as("TenantContext on the listener thread").isEqualTo(777L);
        assertThat(received.principalInThread()).as("principal on the listener thread").isEqualTo("event-tester");
        assertThat(received.event().getActor()).isEqualTo("event-tester");
        assertThat(received.event().getRealm()).isEqualTo(DomainEvent.REALM_STAFF);
        assertThat(received.threadName()).startsWith("erp-event-");
    }

    /**
     * Step 06 + 08: a customer's self-registration (anonymous public endpoint) publishes
     * {@link CustomerRegisteredEvent} in the CUSTOMER realm, actor = the customer itself, after commit.
     */
    @Test
    void customerRegistration_publishesCustomerRegisteredEvent() throws Exception {
        String email = "evt." + UUID.randomUUID().toString().substring(0, 8) + "@shop.test";
        HttpResponse<String> registered = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/public/customers/register"))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Code", "PLATFORM")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email
                    + "\",\"password\":\"Cust0mer-Passw0rd!\",\"fullName\":\"Event Shopper\"}",
                    StandardCharsets.UTF_8))
                .build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);

        DomainEventProbe.Received received = await().atMost(ASYNC_TIMEOUT)
            .until(() -> probe.find(CustomerRegisteredEvent.class, e -> email.equals(e.getEmail())).orElse(null),
                r -> r != null);
        assertThat(received.event().getRealm()).isEqualTo(DomainEvent.REALM_CUSTOMER);
        assertThat(received.event().getTenantId()).isEqualTo(1L);
        assertThat(received.event().getActor()).isEqualTo(email);
        assertThat(((CustomerRegisteredEvent) received.event()).getUserId()).isNotNull();
    }

    @Test
    void theCoreEventExecutor_isNotADefaultCandidate_soTheApplicationsTaskExecutorIsUntouched() {
        // Boot's applicationTaskExecutor is the only TaskExecutor an unqualified injection sees.
        assertThat(defaultTaskExecutors).doesNotContain(eventExecutor);
        assertThat(defaultTaskExecutors).isNotEmpty();
    }
}
