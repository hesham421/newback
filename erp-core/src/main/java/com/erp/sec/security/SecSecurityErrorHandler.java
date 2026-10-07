package com.erp.sec.security;

import com.erp.common.web.FilterErrorResponseWriter;
import com.erp.sec.exception.SecErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * The two filter-chain denials, which never reach {@code GlobalExceptionHandler} because they are
 * raised before the dispatcher: a missing or rejected token (401) and a chain-level access denial
 * (403, SEC-403-FORBIDDEN). Both emit the same {@code ApiResponse} envelope by hand — Jackson's
 * converters are not available this early — with the message resolved from the request's own locale.
 */
@Component
@RequiredArgsConstructor
public class SecSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final MessageSource messageSource;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
            SecErrorCodes.SEC_401_INVALID_CREDENTIALS);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN,
            SecErrorCodes.SEC_403_FORBIDDEN);
    }

    /**
     * The same envelope for any other filter-level refusal of the SEC chains (erp-core step 06:
     * {@link RealmEnforcementFilter}'s 403 {@code REALM_MISMATCH}).
     */
    public void write(HttpServletRequest request, HttpServletResponse response,
                      int status, String errorCode) throws IOException {
        FilterErrorResponseWriter.write(messageSource, request, response, status, errorCode);
    }
}
