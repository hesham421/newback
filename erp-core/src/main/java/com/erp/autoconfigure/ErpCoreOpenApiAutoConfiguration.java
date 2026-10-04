package com.erp.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Per-module springdoc groups, served at {@code /v3/api-docs/{groupId}} next to the aggregate
 * {@code /v3/api-docs}. Active only when springdoc is on the application's classpath (it is an
 * optional dependency of erp-core). Each bean backs off when the application defines one with the
 * same name.
 *
 * <p><b>The group id is a published URL and must stay stable.</b> It is the module code lowercased —
 * never a path prefix or package name. Display names avoid the literal word "Security": the
 * api-doc generator's group match is a substring test, and "se<b>cu</b>rity" contains the module
 * code CU.
 *
 * <p>{@link #erpOpenApi()} declares the one bearer scheme every operation requires, mirroring the
 * core filter chain's {@code anyRequest().authenticated()}; the public auth endpoints opt out per
 * method with an empty {@code @SecurityRequirements}.
 */
@AutoConfiguration
@ConditionalOnClass(GroupedOpenApi.class)
public class ErpCoreOpenApiAutoConfiguration {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI erpOpenApi() {
        return new OpenAPI()
            .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Access token issued by POST /api/v1/sec/auth/login, sent as "
                    + "Authorization: Bearer <token>")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    @ConditionalOnMissingBean(name = "secApi")
    public GroupedOpenApi secApi() {
        return group("sec", "SEC — Identity, Roles & Access", "com.erp.sec.controller");
    }

    @Bean
    @ConditionalOnMissingBean(name = "notifApi")
    public GroupedOpenApi notifApi() {
        return group("notif", "NOTIF — Notification Service", "com.erp.notif.controller");
    }

    @Bean
    @ConditionalOnMissingBean(name = "fileApi")
    public GroupedOpenApi fileApi() {
        return group("file", "FILE — File Service", "com.erp.file.controller");
    }

    @Bean
    @ConditionalOnMissingBean(name = "mdlApi")
    public GroupedOpenApi mdlApi() {
        return group("mdl", "MDL — Master Data Lookup", "com.erp.mdl.controller");
    }

    @Bean
    @ConditionalOnMissingBean(name = "cuApi")
    public GroupedOpenApi cuApi() {
        return group("cu", "CU — Common Utils", "com.erp.cu.controller");
    }

    /**
     * erp-core step 06 — the CUSTOMER realm's endpoints (also listed under {@code sec}, whose package
     * holds their controllers).
     */
    @Bean
    @ConditionalOnMissingBean(name = "customersApi")
    public GroupedOpenApi customersApi() {
        return GroupedOpenApi.builder()
            .group("customers")
            .displayName("Customers — Storefront accounts")
            .pathsToMatch("/api/v1/public/customers/**", "/api/v1/customers/**")
            .build();
    }

    private static GroupedOpenApi group(String id, String displayName, String controllerPackage) {
        return GroupedOpenApi.builder()
            .group(id)
            .displayName(displayName)
            .packagesToScan(controllerPackage)
            .build();
    }
}
