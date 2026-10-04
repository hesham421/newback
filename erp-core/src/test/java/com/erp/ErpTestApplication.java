package com.erp;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * The test-side {@code @SpringBootConfiguration}, named by {@code AbstractIntegrationTest}'s
 * {@code @SpringBootTest(classes = ...)}, so no test depends on the application's main class (which
 * moves to the reference app in step 03). An upward search cannot be used: it scans {@code com.erp}
 * recursively and would find two configurations. It mirrors the production composition: scan
 * all of {@code com.erp}, with Hibernate's JPA auto-configuration excluded because
 * {@code JpaConfig} builds the EntityManagerFactory itself.
 *
 * <p>Any other {@code @SpringBootConfiguration} found by the scan (the production main class) is
 * filtered out so its auto-configuration and scan are not processed a second time. Test-only
 * {@code @TestConfiguration} classes are skipped by {@link TypeExcludeFilter} and must be imported
 * explicitly (as {@code AbstractIntegrationTest} does).
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = HibernateJpaAutoConfiguration.class)
@ComponentScan(basePackages = "com.erp", excludeFilters = {
    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
    @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
})
public class ErpTestApplication {
}
