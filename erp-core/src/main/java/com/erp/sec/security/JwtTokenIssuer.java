package com.erp.sec.security;

import com.erp.sec.entity.User;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues the API-SEC-001 access token. Issuance only — request-time validation, the filter chain
 * and the REQ-SEC-033 gateway belong to the SEC-BE phase. The token's {@code jti} is the session's
 * opaque {@code tokenRef} (DBF-SEC-077), so no raw token or hash is ever persisted.
 */
@Component
public class JwtTokenIssuer {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenIssuer(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String issue(User user, String tokenRef, Instant issuedAt) {
        return Jwts.builder()
            .subject(user.getUsername())
            .id(tokenRef)
            .claim("uid", user.getUserPk())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plusMillis(expirationMs)))
            .signWith(signingKey)
            .compact();
    }

    /** {@code LoginResponse.expiresIn} is seconds, while {@code app.jwt.expiration-ms} is millis. */
    public long getExpiresInSeconds() {
        return expirationMs / 1000L;
    }
}
