package com.erp;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Test fixture (review case B): a consumer whose configuration lives in {@code com.erp}, a parent of
 * every core package, so its auto-configuration package overlaps the core packages.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
public class ErpRootConsumerApplication {
}
