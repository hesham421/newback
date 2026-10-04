package com.erp.main.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Per-module springdoc groups. Without these the application publishes a single ungrouped
 * document at {@code /v3/api-docs} covering every module at once, which
 * {@code governance/governance-tools/api-doc-generator/} cannot publish per module: its
 * {@code discovery.match_openapi_group} identifies "which module is which" by matching the
 * {@code --module} argument against a {@code GroupedOpenApi} bean's method name, group id or
 * display name (discovery.py header: "erp-main's GroupedOpenApi bean declarations for the
 * springdoc group id, package(s), and display name that identify 'which module is ORG'").
 *
 * <p>Each group is served at {@code /v3/api-docs/{groupId}} alongside the unchanged aggregate
 * document at {@code /v3/api-docs}; {@code springdoc.api-docs.path} and {@code server.port} in
 * {@code application.properties} are untouched.
 *
 * <p><b>The group id is a published URL and the generator's {@code --module} match key, so it
 * must stay stable forever.</b> It is the governance module code lowercased — never a path
 * prefix, never a package name — so renaming a controller or a route can never move a module's
 * documentation URL. Note that CU's live route prefix is {@code /api/v1/common} while its module
 * code is {@code CU}; the group id follows the module code.
 *
 * <p>Display names deliberately avoid the literal word "Security": the generator's match is a
 * substring test over normalised text, and "se<b>cu</b>rity" contains the module code CU, which
 * would make {@code --module CU} ambiguous between two groups and silently drop CU's
 * source-derived sections.
 *
 * <p>{@link #erpOpenApi()} declares the one bearer scheme every group inherits and applies it to
 * every operation, mirroring {@code SecurityConfig}'s {@code anyRequest().authenticated()}. Until it
 * existed no operation carried a {@code security} requirement, so every generated endpoint page read
 * "Authentication: Not determined" and the generator's {@code check} failed {@code auth-determined}
 * on all six modules. The four public auth endpoints opt out per method with an empty
 * {@code @SecurityRequirements}, exactly the paths {@code SecurityConfig} lists as {@code permitAll}.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
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
    public GroupedOpenApi secApi() {
        return GroupedOpenApi.builder()
            .group("sec")
            .displayName("SEC — Identity, Roles & Access")
            .packagesToScan("com.erp.sec.controller")
            .build();
    }

    @Bean
    public GroupedOpenApi cuApi() {
        return GroupedOpenApi.builder()
            .group("cu")
            .displayName("CU — Common Utils")
            .packagesToScan("com.erp.cu.controller")
            .build();
    }

    @Bean
    public GroupedOpenApi notifApi() {
        return GroupedOpenApi.builder()
            .group("notif")
            .displayName("NOTIF — Notification Service")
            .packagesToScan("com.erp.notif.controller")
            .build();
    }

    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
            .group("file")
            .displayName("FILE — File Service")
            .packagesToScan("com.erp.file.controller")
            .build();
    }

    @Bean
    public GroupedOpenApi mdlApi() {
        return GroupedOpenApi.builder()
            .group("mdl")
            .displayName("MDL — Master Data Lookup")
            .packagesToScan("com.erp.mdl.controller")
            .build();
    }
}
