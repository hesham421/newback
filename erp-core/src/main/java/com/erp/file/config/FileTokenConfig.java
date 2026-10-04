package com.erp.file.config;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.file.domain.FileAccessTokenDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@link FileAccessTokenDomainService} (a plain, secret-derived crypto helper — not itself a
 * Spring stereotype) as a singleton bean so the FILE service can inject it. The AES key is derived
 * from {@code erp.core.files.access-token-secret} (RULE-FILE-003).
 */
@Configuration
public class FileTokenConfig {

    @Bean
    public FileAccessTokenDomainService fileAccessTokenDomainService(ErpCoreProperties properties) {
        return FileAccessTokenDomainService.create(properties.getFiles().getAccessTokenSecret());
    }
}
