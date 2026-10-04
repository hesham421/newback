package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.TokenHasher;
import com.erp.sec.dto.DevPasswordResetTokenResponse;
import com.erp.sec.entity.PasswordResetToken;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.repository.PasswordResetTokenRepository;
import com.erp.sec.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test fixture for TC-SEC-038, which needs a usable reset token and cannot get one from
 * API-SEC-003: only the SHA-256 hash is persisted, so the raw value an automated run would have to
 * submit exists nowhere after the mail is dispatched. Mints one directly instead — same entity,
 * same hashing, same A7 30-minute window as {@code PasswordResetService} — and returns it. It
 * deliberately sends no mail and writes no PASSWORD_RESET_REQUESTED audit row: it is not a second
 * implementation of API-SEC-003, which is separately covered and passing, only a way to obtain a
 * token API-SEC-004 will accept.
 *
 * <p>{@code @Profile("dev")} is the containment: outside the dev profile the bean does not exist,
 * so neither does the endpoint. It must never be given one in any other profile.
 */
@Service
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevPasswordResetSupportService {

    private final PasswordResetTokenRepository repository;
    private final UserRepository userRepository;

    /**
     * Unlike API-SEC-003 this does reveal whether an address is registered — the no-enumeration
     * property of the real endpoint is not weakened, because this one exists only in dev and is
     * reachable only by an already-authenticated caller.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<DevPasswordResetTokenResponse> issueToken(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_USER, email));

        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken saved = repository.save(PasswordResetToken.builder()
            .user(user)
            .tokenHash(TokenHasher.sha256Hex(rawToken))
            .build());

        log.warn("DEV-ONLY: issued a password-reset token for User ID {} through the test-fixture "
            + "endpoint", user.getUserPk());

        return ServiceResult.success(DevPasswordResetTokenResponse.builder()
            .token(rawToken)
            .expiresAt(saved.getExpiresAt())
            .build());
    }
}
