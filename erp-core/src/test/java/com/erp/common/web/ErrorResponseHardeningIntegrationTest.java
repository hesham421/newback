package com.erp.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * erp-core 1.2.0 — client and programming errors that used to surface as a generic 500
 * {@code INTERNAL_ERROR}: an unknown path (404 {@code NOT_FOUND}), a page number whose offset
 * overflows (400 {@code VALIDATION_ERROR} on {@code page}), and {@code TENANT_CONTEXT_MISSING} wrapped
 * by the transaction manager (500 carrying its own code).
 */
class ErrorResponseHardeningIntegrationTest extends AbstractIntegrationTest {

    private static final String CONFIGS_SEARCH = "/api/v1/common/configurations/search";

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    private StaffApiClient http;
    private String token;

    @BeforeEach
    void login() {
        http = new StaffApiClient(port);
        token = http.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder));
    }

    @Test
    void anUnknownPath_forAnAuthenticatedCaller_is404NotFound() {
        HttpResponse<String> response = http.get(token, "/api/v1/no-such-module/no-such-endpoint");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(StaffApiClient.errorCode(response)).isEqualTo(CommonErrorCodes.NOT_FOUND);
        assertThat((String) JsonPath.read(response.body(), "$.error.message"))
            .isEqualTo("The requested resource was not found");
    }

    @Test
    void anUnknownPath_underAPermittedPrefix_forAnAnonymousCaller_is404NotFound() {
        HttpResponse<String> response = http.get(null, "/swagger-ui/no-such-file.png");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(StaffApiClient.errorCode(response)).isEqualTo(CommonErrorCodes.NOT_FOUND);
    }

    @Test
    void anUnknownPath_underAProtectedPrefix_forAnAnonymousCaller_staysUnauthorized() {
        HttpResponse<String> response = http.get(null, "/api/v1/no-such-module/no-such-endpoint");

        assertThat(response.statusCode()).as("authentication is decided before routing").isEqualTo(401);
    }

    @Test
    void aPageWhoseOffsetOverflows_is400ValidationErrorOnPage() {
        HttpResponse<String> response = http.post(token, CONFIGS_SEARCH, "{\"page\":2147483647,\"size\":20}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(StaffApiClient.errorCode(response)).isEqualTo(CommonErrorCodes.VALIDATION_ERROR);
        assertThat((String) JsonPath.read(response.body(), "$.error.fieldErrors[0].field")).isEqualTo("page");
    }

    @Test
    void aLargeButRepresentablePage_isAnEmptyPage() {
        HttpResponse<String> response = http.post(token, CONFIGS_SEARCH, "{\"page\":1000000,\"size\":20}");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void tenantContextMissing_wrappedByTheTransactionManager_isAnsweredWithItsOwnCode() throws Exception {
        AtomicReference<Exception> failure = new AtomicReference<>();
        Thread job = new Thread(() -> {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> { });
            } catch (Exception e) {
                failure.set(e);
            }
        }, "no-tenant-job");
        job.start();
        job.join(30_000);

        Exception error = failure.get();
        assertThat(error).as("a transaction without a tenant fails").isNotNull()
            .isNotInstanceOf(LocalizedException.class);   // wrapped, which is what used to hide the code

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnexpected(error);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getCode()).isEqualTo(TenantErrorCodes.TENANT_CONTEXT_MISSING);
    }
}
