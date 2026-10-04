package com.erp.testsupport;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The test-side application for erp-core's integration tests, named by
 * {@link AbstractIntegrationTest}. It scans only {@code com.erp.testsupport} — none of the core
 * packages — so the core is wired exclusively by its auto-configuration
 * ({@code META-INF/spring/...AutoConfiguration.imports}), exactly as in a consuming application.
 * Test-only {@code @TestConfiguration} classes are skipped by the scan and must be imported
 * explicitly (as {@link AbstractIntegrationTest} does).
 */
@SpringBootApplication(scanBasePackages = "com.erp.testsupport")
public class CoreTestApplication {
}
