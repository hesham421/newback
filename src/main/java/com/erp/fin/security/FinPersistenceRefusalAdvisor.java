package com.erp.fin.security;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.exception.FinErrorCodes;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * ADR-FIN-030: answers a store's uniqueness refusal on a FIN service as the business error it
 * stands for, from a fixed constraint-name registry; any other refusal is re-thrown unchanged.
 * Ordered outside the transaction interceptor so a refusal raised at flush or at commit passes
 * through it. Same shape as {@link FinForbiddenAdvisor}; never a catch inside a service.
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Slf4j
public class FinPersistenceRefusalAdvisor extends DefaultPointcutAdvisor {

    private static final String FIN_SERVICE_PACKAGE = "com.erp.fin.service.";

    private static final Map<String, String> CONSTRAINT_ERROR_CODES = Map.of(
        "UQ_FIN_JOURNAL_ENTRY_EVENT_REF", FinErrorCodes.FIN_409_DUPLICATE_EVENT,
        "UQ_FIN_ACCOUNT_MAPPING_ACTIVE_KEY", FinErrorCodes.FIN_409_MAPPING_DUP
    );

    public FinPersistenceRefusalAdvisor() {
        setPointcut(new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return targetClass != null && targetClass.getName().startsWith(FIN_SERVICE_PACKAGE);
            }
        });
        setAdvice((MethodInterceptor) invocation -> {
            try {
                return invocation.proceed();
            } catch (DataIntegrityViolationException refused) {
                String constraintName = violatedConstraintName(refused);
                String errorCode = constraintName == null
                    ? null
                    : CONSTRAINT_ERROR_CODES.get(constraintName.toUpperCase(Locale.ROOT));
                if (errorCode == null) {
                    throw refused;
                }
                log.warn("Store refused {} on {}: {}", constraintName,
                    invocation.getMethod().getName(), errorCode);
                throw new LocalizedException(Status.CONFLICT, errorCode);
            }
        });
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    /** PostgreSQL reports the identifier lower-cased; the registry lookup upper-cases it. */
    private static String violatedConstraintName(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return null;
    }
}
