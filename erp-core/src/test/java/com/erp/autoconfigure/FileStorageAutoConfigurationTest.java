package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.erp.file.repository.FileDocumentRepository;
import com.erp.file.storage.DbStorageProvider;
import com.erp.file.storage.LocalFsStorageProvider;
import com.erp.file.storage.S3StorageProvider;
import com.erp.file.storage.StorageProviderRegistry;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * erp-core step 07 task 3 — storage selection and its startup validation: DB by default; LOCAL needs
 * an existing, writable root; S3 needs a bucket and the SDK; an unknown value fails the startup.
 */
class FileStorageAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class))
        .withUserConfiguration(Properties.class)
        .withBean(FileDocumentRepository.class, () -> mock(FileDocumentRepository.class))
        .withPropertyValues(
            "erp.core.security.jwt.secret=test-only-jwt-secret-0123456789abcdef0123456789abcdef",
            "erp.core.files.access-token-secret=test-only-file-secret-0123456789abcdef0123456789");

    @TempDir
    Path tempDir;

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ErpCoreProperties.class)
    static class Properties {
    }

    @Test
    void default_isTheDbProvider_andLocalIsAbsentWithoutARoot() {
        runner.run(context -> {
            StorageProviderRegistry registry = context.getBean(StorageProviderRegistry.class);
            assertThat(registry.active()).isInstanceOf(DbStorageProvider.class);
            assertThat(registry.keys()).containsExactly("DB");
        });
    }

    @Test
    void local_withAWritableRoot_isSelected_andDbStaysAvailableForOlderDocuments() {
        runner.withPropertyValues("erp.core.files.storage=local", "erp.core.files.local.root=" + tempDir)
            .run(context -> {
                StorageProviderRegistry registry = context.getBean(StorageProviderRegistry.class);
                assertThat(registry.active()).isInstanceOf(LocalFsStorageProvider.class);
                assertThat(((LocalFsStorageProvider) registry.active()).root()).isEqualTo(tempDir.toAbsolutePath().normalize());
                assertThat(registry.keys()).containsExactlyInAnyOrder("DB", "LOCAL");
                assertThat(registry.forKey("DB")).isInstanceOf(DbStorageProvider.class);
            });
    }

    @Test
    void local_withoutRoot_orWithAMissingRoot_failsTheStartup() {
        runner.withPropertyValues("erp.core.files.storage=LOCAL").run(context ->
            assertThat(context).getFailure().rootCause().hasMessageContaining("erp.core.files.local.root"));
        runner.withPropertyValues("erp.core.files.storage=LOCAL",
                "erp.core.files.local.root=" + tempDir.resolve("does-not-exist"))
            .run(context -> assertThat(context).getFailure().rootCause()
                .hasMessageContaining("erp.core.files.local.root must be an existing, writable directory"));
    }

    @Test
    void unknownStorage_failsTheStartup() {
        runner.withPropertyValues("erp.core.files.storage=FTP").run(context ->
            assertThat(context).getFailure().rootCause().hasMessageContaining("erp.core.files.storage must be DB, LOCAL or S3"));
    }

    @Test
    void s3_withBucket_usesAnApplicationS3Client_withoutTouchingTheNetwork() {
        runner.withBean(S3Client.class, () -> new S3StorageProviderTestClient())
            .withPropertyValues("erp.core.files.storage=S3", "erp.core.files.s3.bucket=erp-files",
                "erp.core.files.s3.public-base-url=https://cdn.example.com")
            .run(context -> {
                StorageProviderRegistry registry = context.getBean(StorageProviderRegistry.class);
                assertThat(registry.active()).isInstanceOf(S3StorageProvider.class);
                assertThat(registry.active().publicUrl("k")).hasValue("https://cdn.example.com/k");
            });
    }

    @Test
    void s3_withoutBucket_failsTheStartup() {
        runner.withBean(S3Client.class, () -> new S3StorageProviderTestClient())
            .withPropertyValues("erp.core.files.storage=S3")
            .run(context -> assertThat(context).getFailure().rootCause()
                .hasMessageContaining("erp.core.files.s3.bucket"));
    }

    @Test
    void s3_selectedWithoutTheSdk_failsTheStartup_whileDbWorksWithoutIt() {
        ApplicationContextRunner withoutSdk = runner.withClassLoader(new FilteredClassLoader(S3Client.class));
        withoutSdk.withPropertyValues("erp.core.files.storage=S3", "erp.core.files.s3.bucket=erp-files")
            .run(context -> assertThat(context).getFailure().rootCause()
                .hasMessageContaining("needs software.amazon.awssdk:s3"));
        withoutSdk.run(context ->
            assertThat(context.getBean(StorageProviderRegistry.class).active()).isInstanceOf(DbStorageProvider.class));
    }

    /** An S3Client that is never called (the registry only wires it). */
    static final class S3StorageProviderTestClient implements S3Client {

        @Override
        public String serviceName() {
            return "s3";
        }

        @Override
        public void close() {
        }
    }
}
