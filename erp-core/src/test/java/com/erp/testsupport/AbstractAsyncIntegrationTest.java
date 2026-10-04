package com.erp.testsupport;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.erp.events.ErpCoreEvents;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base of the erp-core step 08 asynchronous tests (event bus, NOTIF delivery): one shared context with
 * a Mockito {@link JavaMailSender} (so the core {@code EmailChannelProvider} exists and "sends" without
 * SMTP) and the {@link DomainEventProbe} listener.
 *
 * <p>Asynchronous outcomes are awaited with Awaitility (polling the database or the probe until the
 * expected state appears, bounded by a timeout) — never with sleeps-as-assertions. Retries use the
 * test profile's millisecond backoff. After each test the core event executor is drained, so no
 * delivery of one test is still running when the next one starts.
 */
@Import(DomainEventProbe.Config.class)
public abstract class AbstractAsyncIntegrationTest extends AbstractIntegrationTest {

    /** Upper bound for any asynchronous outcome in these tests. */
    protected static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(20);

    @MockitoBean
    protected JavaMailSender mailSender;

    @Autowired
    protected DomainEventProbe probe;

    @Autowired
    @Qualifier(ErpCoreEvents.EXECUTOR)
    protected ThreadPoolTaskExecutor eventExecutor;

    @BeforeEach
    void resetAsyncFixtures() {
        reset(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        probe.clear();
    }

    @AfterEach
    void drainEventExecutor() {
        SecurityContextHolder.clearContext();
        awaitExecutorIdle();
    }

    /** Waits until the core event executor runs nothing and has nothing queued. */
    protected void awaitExecutorIdle() {
        await().atMost(ASYNC_TIMEOUT).until(() ->
            eventExecutor.getActiveCount() == 0 && eventExecutor.getThreadPoolExecutor().getQueue().isEmpty());
    }
}
