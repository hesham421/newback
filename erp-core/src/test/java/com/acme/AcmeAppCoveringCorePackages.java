package com.acme;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Test fixture: a consumer whose own {@code @EnableJpaRepositories} covers the core packages. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = {"com.erp", "com.acme"})
public class AcmeAppCoveringCorePackages {
}
