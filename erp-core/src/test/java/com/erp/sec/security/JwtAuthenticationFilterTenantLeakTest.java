package com.erp.sec.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.service.MenuService;
import com.erp.tenant.TenantContext;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * erp-core 1.2.0 — a tenant left on a reused worker thread is never seen by the request: the JWT
 * filter (the outermost tenant-aware filter of both core chains) clears it before anything else and
 * does not put it back, so the thread leaves the filter clean (nothing that runs on it later outside
 * the filter, such as a container error dispatch, can pick the leak up).
 */
class JwtAuthenticationFilterTenantLeakTest {

    private static final long LEAKED = 999L;

    private final JwtTokenValidator tokenValidator = mock(JwtTokenValidator.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenValidator,
        mock(UserRepository.class), mock(ActiveSessionRepository.class), mock(MenuService.class));

    @BeforeEach
    void leakATenant() {
        SecurityContextHolder.clearContext();
        TenantContext.set(LEAKED);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void aLeakedTenant_isNotVisibleToAnAnonymousRequest_andIsGoneAfterTheChain() throws Exception {
        AtomicReference<Long> seen = new AtomicReference<>(-1L);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/anything"), new MockHttpServletResponse(),
            (request, response) -> seen.set(TenantContext.current()));

        assertThat(seen.get()).as("tenant inside the chain").isNull();
        assertThat(TenantContext.current()).as("not restored after the chain").isNull();
    }

    @Test
    void aLeakedTenant_isNotVisibleToARequestWithARejectedToken() throws Exception {
        when(tokenValidator.parse(anyString())).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        request.addHeader("Authorization", "Bearer not-a-valid-token");
        AtomicReference<Long> seen = new AtomicReference<>(-1L);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.set(TenantContext.current()));

        assertThat(seen.get()).isNull();
        assertThat(TenantContext.current()).isNull();
    }

    @Test
    void aCleanThread_staysClean() throws Exception {
        TenantContext.clear();
        AtomicReference<Long> seen = new AtomicReference<>(-1L);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/anything"), new MockHttpServletResponse(),
            (request, response) -> seen.set(TenantContext.current()));

        assertThat(seen.get()).isNull();
        assertThat(TenantContext.current()).isNull();
    }
}
