package com.erp.sec.service;

import com.erp.events.TenantSuspendedEvent;
import com.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * REQ-SEC-092 (tenant-maturity C12, XM-SEC-007) — ends every open session of a suspended tenant, both realms, after the
 * suspension commits. Synchronous (the operator's request), inside the event's tenant and a {@code REQUIRES_NEW}
 * transaction (the committed one is still bound); a failure is logged and never undoes the suspension.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSuspendedSessionListener {

    private final UserSessionTerminator sessionTerminator;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTenantSuspended(TenantSuspendedEvent event) {
        Long tenantId = event.getTenantId();
        if (tenantId == null) {
            log.error("{} carries no tenant — no session is terminated", event);
            return;
        }
        TransactionTemplate inTenant = new TransactionTemplate(transactionManager);
        inTenant.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String operator = event.getActor();
        try {
            Integer ended = TenantContext.callAs(tenantId, () -> inTenant.execute(status ->
                sessionTerminator.terminateAllOpenSessions(operator,
                    "إنهاء الجلسة: علّق مشغّل المنصة " + operator + " المستأجر",
                    "Session terminated: the tenant was suspended by the platform operator " + operator)));
            log.info("Tenant ID: {} suspended — {} open session(s) terminated", tenantId, ended);
        } catch (RuntimeException e) {
            log.error("The sessions of suspended tenant ID: {} could not be terminated — its tokens are refused by the"
                + " tenant filter anyway", tenantId, e);
        }
    }
}
