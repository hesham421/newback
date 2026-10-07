package com.erp.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.MediaType;

/**
 * Writes the {@link ApiResponse} error envelope by hand, for refusals raised by a servlet filter —
 * before the dispatcher, so neither {@link GlobalExceptionHandler} nor Jackson's converters are
 * involved. The message is resolved from the request's own locale ({@code request.getLocale()}, not
 * {@code LocaleContextHolder}, which is not populated yet); an unknown code is used as its own message.
 */
public final class FilterErrorResponseWriter {

    private FilterErrorResponseWriter() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static void write(MessageSource messageSource, HttpServletRequest request,
                             HttpServletResponse response, int status, String errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
            "{\"success\":false,\"error\":{\"code\":\"" + errorCode
                + "\",\"message\":\"" + escape(resolve(messageSource, errorCode, request))
                + "\"},\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private static String resolve(MessageSource messageSource, String errorCode, HttpServletRequest request) {
        try {
            return messageSource.getMessage(errorCode, null, request.getLocale());
        } catch (NoSuchMessageException e) {
            return errorCode;
        }
    }

    private static String escape(String message) {
        return message.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
