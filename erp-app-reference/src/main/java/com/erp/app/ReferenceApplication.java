package com.erp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The reference consumer of erp-core. It scans only its own package ({@code com.erp.app}); everything
 * erp-core provides (SEC, CU, MDL, FILE, NOTIF, security chain, Flyway core chain, OpenAPI groups)
 * arrives through erp-core's auto-configuration, exactly as in any other application that adds
 * {@code com.erp:erp-core} as a dependency.
 */
@SpringBootApplication
public class ReferenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferenceApplication.class, args);
    }
}
