package com.acme;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/** Test fixture: a consumer in {@code com.acme} that relies on auto-configuration for its repositories. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
public class AcmeAppWithoutRepositoryConfig {
}
