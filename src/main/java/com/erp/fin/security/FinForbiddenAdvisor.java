package com.erp.fin.security;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.exception.FinErrorCodes;
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
 * The Error Catalog's {@code FIN-403-FORBIDDEN} row: a denial on a FIN service must answer FIN's
 * own catalog code, not the shared handler's platform {@code ACCESS_DENIED}. Ordered ahead of the
 * {@code @PreAuthorize} interceptor so it sees that denial and re-raises it as the catalog error
 * the shared handler already maps. Declared once for the whole module — never repeated per
 * endpoint, and never as a feature-module {@code @ControllerAdvice} (gov-enforce-backend-contract
 * CU.7 forbids one; {@code GlobalExceptionHandler} is the only advice in the project). Exact
 * mirror of {@code com.erp.sec.security.SecForbiddenAdvisor}, the proven precedent.
 *
 * <p><b>What this does NOT cover.</b> Only denials raised inside {@code com.erp.fin.service} are
 * translated. A denial raised by the Spring Security filter chain — before any FIN service is
 * entered — never reaches an AOP proxy or {@code GlobalExceptionHandler} at all: it is written
 * directly by {@code SecSecurityErrorHandler} (wired as the chain's {@code accessDeniedHandler} in
 * {@code SecurityConfig}) and still answers {@code SEC-403-FORBIDDEN}, platform-wide. Today
 * {@code SecurityConfig} authorizes FIN paths with {@code .anyRequest().authenticated()} only, so
 * every FIN permission denial is in fact a {@code @PreAuthorize} denial on a FIN service and does
 * pass through here; that would stop being true if a URL-level authority rule were ever added.
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Slf4j
public class FinForbiddenAdvisor extends DefaultPointcutAdvisor {

    private static final String FIN_SERVICE_PACKAGE = "com.erp.fin.service.";

    public FinForbiddenAdvisor() {
        setPointcut(new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return targetClass != null && targetClass.getName().startsWith(FIN_SERVICE_PACKAGE);
            }
        });
        setAdvice((MethodInterceptor) invocation -> {
            try {
                return invocation.proceed();
            } catch (AccessDeniedException denied) {
                log.warn("Authorization denied on {}", invocation.getMethod().getName());
                throw new LocalizedException(Status.FORBIDDEN, FinErrorCodes.FIN_403_FORBIDDEN);
            }
        });
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
