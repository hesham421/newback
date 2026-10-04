package com.acme;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Test fixture (review case A): a consumer in {@code com.acme} that registers its own repositories. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "com.acme")
public class AcmeAppWithOwnRepositories {
}
