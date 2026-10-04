package com.erp.sec.security;

import com.erp.autoconfigure.ErpCoreProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * The request-time counterpart of {@link JwtTokenIssuer}, on the same {@code erp.core.security.jwt.secret} key.
 * Signature and expiry are both decided by the parser; a rejected token yields an empty result
 * rather than an exception, and the token itself is never logged (POL-SEC-004).
 */
@Component
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(ErpCoreProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(
            properties.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
