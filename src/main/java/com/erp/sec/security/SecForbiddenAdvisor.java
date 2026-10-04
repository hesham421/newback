package com.erp.sec.security;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.exception.SecErrorCodes;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * SEC-BE.md "Forbidden responses": a denial on a SEC service must answer SEC-403-FORBIDDEN, not the
 * shared handler's generic ACCESS_DENIED. Ordered ahead of the {@code @PreAuthorize} interceptor so
 * it sees that denial and re-raises it as the catalog error the shared handler already maps.
 * Declared once for the whole module — never repeated per endpoint (CORE.md REQ-SEC-033).
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Slf4j
public class SecForbiddenAdvisor extends DefaultPointcutAdvisor {

    private static final String SEC_SERVICE_PACKAGE = "com.erp.sec.service.";

    public SecForbiddenAdvisor() {
        setPointcut(new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return targetClass != null && targetClass.getName().startsWith(SEC_SERVICE_PACKAGE);
            }
        });
        setAdvice((MethodInterceptor) invocation -> {
            try {
                return invocation.proceed();
            } catch (AccessDeniedException denied) {
                log.warn("Authorization denied on {}", invocation.getMethod().getName());
                throw new LocalizedException(Status.FORBIDDEN, SecErrorCodes.SEC_403_FORBIDDEN);
            }
        });
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
