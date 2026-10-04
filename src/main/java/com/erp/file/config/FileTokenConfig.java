package com.erp.file.config;

import com.erp.file.domain.FileAccessTokenDomainService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@link FileAccessTokenDomainService} (a plain, secret-derived crypto helper — not itself a
 * Spring stereotype) as a singleton bean so the FILE service can inject it. The AES key is derived
 * from {@code file.access-token.secret} (RULE-FILE-003).
 */
@Configuration
public class FileTokenConfig {

    @Bean
    public FileAccessTokenDomainService fileAccessTokenDomainService(
            @Value("${file.access-token.secret}") String secret) {
        return FileAccessTokenDomainService.create(secret);
    }
}
