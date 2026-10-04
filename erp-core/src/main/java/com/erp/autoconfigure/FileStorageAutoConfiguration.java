package com.erp.autoconfigure;

import com.erp.file.repository.FileDocumentRepository;
import com.erp.file.storage.DbStorageProvider;
import com.erp.file.storage.LocalFsStorageProvider;
import com.erp.file.storage.S3StorageProvider;
import com.erp.file.storage.StorageKeys;
import com.erp.file.storage.StorageProvider;
import com.erp.file.storage.StorageProviderRegistry;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * FILE storage providers (erp-core step 07) and the {@link StorageProviderRegistry} that picks the
 * one selected by {@code erp.core.files.storage} ({@code DB} by default) for new uploads:
 * <ul>
 *   <li>{@code DB} — {@link DbStorageProvider}, always available (it is also what every document
 *       written before step 07 uses);</li>
 *   <li>{@code LOCAL} — {@link LocalFsStorageProvider}, available when {@code erp.core.files.local.root}
 *       is set; when selected, the root must exist, be a directory and be writable;</li>
 *   <li>{@code S3} — {@link S3StorageProvider}, available when {@code software.amazon.awssdk:s3} is on
 *       the classpath and {@code erp.core.files.s3.bucket} is set (an application may supply its own
 *       {@link S3Client} bean).</li>
 * </ul>
 * Startup fails with a message naming the property when the selection is unknown or its provider
 * cannot be built. An application may replace a built-in provider by contributing a
 * {@link StorageProvider} bean with the same key (beans win over the built-in LOCAL/S3), or replace the
 * registry. The keys are limited to DB/LOCAL/S3 by {@code CHK_FILE_DOCUMENT_STORAGE_PROVIDER}.
 */
@AutoConfiguration
public class FileStorageAutoConfiguration {

    public static final String DB_STORAGE_PROVIDER_BEAN = "erpDbStorageProvider";

    @Bean(name = DB_STORAGE_PROVIDER_BEAN)
    @ConditionalOnMissingBean(name = DB_STORAGE_PROVIDER_BEAN)
    public DbStorageProvider erpDbStorageProvider(@Lazy FileDocumentRepository repository) {
        return new DbStorageProvider(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public StorageProviderRegistry storageProviderRegistry(ErpCoreProperties properties,
                                                          List<StorageProvider> providers,
                                                          ObjectProvider<S3StorageProviderFactory> s3Factory) {
        ErpCoreProperties.Files files = properties.getFiles();
        String selected = files.getStorage() == null ? "" : files.getStorage().trim().toUpperCase(Locale.ROOT);
        if (!List.of(StorageKeys.DB, StorageKeys.LOCAL, StorageKeys.S3).contains(selected)) {
            throw new IllegalStateException("erp.core.files.storage must be DB, LOCAL or S3 (was '"
                + files.getStorage() + "')");
        }

        List<StorageProvider> all = new ArrayList<>(providers);
        LocalFsStorageProvider local = localProvider(files, StorageKeys.LOCAL.equals(selected));
        if (local != null) {
            all.add(local);
        }
        if (StorageKeys.S3.equals(selected) || StringUtils.hasText(files.getS3().getBucket())) {
            S3StorageProviderFactory factory = s3Factory.getIfAvailable();
            if (factory == null) {
                if (StorageKeys.S3.equals(selected)) {
                    throw new IllegalStateException("erp.core.files.storage=S3 needs software.amazon.awssdk:s3 on the "
                        + "classpath (an optional dependency of erp-core)");
                }
            } else {
                all.add(factory.create(files.getS3(), StorageKeys.S3.equals(selected)));
            }
        }
        return new StorageProviderRegistry(all, selected);
    }

    /**
     * The LOCAL provider when a root is configured; a selected LOCAL without a usable root fails the
     * startup (task 3: the root must exist and be writable).
     */
    static LocalFsStorageProvider localProvider(ErpCoreProperties.Files files, boolean selected) {
        String rootSetting = files.getLocal().getRoot();
        if (!StringUtils.hasText(rootSetting)) {
            if (selected) {
                throw new IllegalStateException("erp.core.files.storage=LOCAL needs erp.core.files.local.root "
                    + "(an existing, writable directory)");
            }
            return null;
        }
        Path root = Path.of(rootSetting.trim()).toAbsolutePath().normalize();
        if (selected && !(Files.isDirectory(root) && Files.isWritable(root))) {
            throw new IllegalStateException("erp.core.files.local.root must be an existing, writable directory: "
                + root);
        }
        return new LocalFsStorageProvider(root);
    }

    /**
     * Builds the S3 provider. Typed on {@link StorageProvider} (not {@code S3StorageProvider}) so that
     * nothing outside {@link S3Configuration} references a class that links against the AWS SDK.
     */
    public interface S3StorageProviderFactory {

        StorageProvider create(ErpCoreProperties.S3 settings, boolean selected);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
    static class S3Configuration {

        @Bean
        @ConditionalOnMissingBean
        S3StorageProviderFactory s3StorageProviderFactory(ObjectProvider<S3Client> applicationClient) {
            return (settings, selected) -> {
                if (!StringUtils.hasText(settings.getBucket())) {
                    throw new IllegalStateException("erp.core.files.storage=S3 needs erp.core.files.s3.bucket");
                }
                S3Client client = applicationClient.getIfAvailable(() -> buildClient(settings));
                return new S3StorageProvider(client, settings.getBucket().trim(), settings.getPublicBaseUrl());
            };
        }

        static S3Client buildClient(ErpCoreProperties.S3 settings) {
            S3ClientBuilder builder = S3Client.builder().region(Region.of(settings.getRegion()));
            if (StringUtils.hasText(settings.getEndpoint())) {
                builder.endpointOverride(URI.create(settings.getEndpoint().trim())).forcePathStyle(true);
            }
            if (StringUtils.hasText(settings.getAccessKey())) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(settings.getAccessKey(), settings.getSecretKey())));
            }
            return builder.build();
        }
    }
}
