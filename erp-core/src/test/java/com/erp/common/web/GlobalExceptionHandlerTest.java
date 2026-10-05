package com.erp.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** erp-core 1.2.0 — the new 404 mapping and the unwrapping of a wrapped {@link LocalizedException}. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messages());

    @Test
    void noResourceFound_is404NotFound_inEnglishAndArabic() {
        NoResourceFoundException missing = new NoResourceFoundException(HttpMethod.GET, "/api/v1/nope", "api/v1/nope");

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            ResponseEntity<ApiResponse<Void>> en = handler.handleNoResource(missing);
            assertThat(en.getStatusCode().value()).isEqualTo(404);
            assertThat(en.getBody().getError().getCode()).isEqualTo(CommonErrorCodes.NOT_FOUND);
            assertThat(en.getBody().getError().getMessage()).isEqualTo("The requested resource was not found");

            LocaleContextHolder.setLocale(Locale.forLanguageTag("ar"));
            ResponseEntity<ApiResponse<Void>> ar = handler.handleNoResource(missing);
            assertThat(ar.getBody().getError().getMessage()).isEqualTo("المورد المطلوب غير موجود");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void aWrappedLocalizedException_isAnsweredWithItsOwnCodeAndStatus() {
        LocalizedException cause = new LocalizedException(Status.INTERNAL_ERROR, "TENANT_CONTEXT_MISSING");
        Exception wrapped = new CannotCreateTransactionException("Could not open JPA EntityManager",
            new IllegalStateException("hibernate", cause));

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(wrapped);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getCode()).isEqualTo("TENANT_CONTEXT_MISSING");
    }

    @Test
    void aWrappedClientError_keepsItsClientStatus() {
        Exception wrapped = new RuntimeException("wrapper",
            new LocalizedException(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR));

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(wrapped);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getCode()).isEqualTo(CommonErrorCodes.VALIDATION_ERROR);
    }

    @Test
    void anythingElse_staysInternalError() {
        ResponseEntity<ApiResponse<Void>> response =
            handler.handleUnexpected(new IllegalStateException("boom", new RuntimeException("cause")));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getCode()).isEqualTo(CommonErrorCodes.INTERNAL_ERROR);
    }

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
