package com.erp.file.storage;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.exception.FileErrorCodes;
import java.io.InputStream;
import java.time.Clock;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * The optional {@code S3} provider (any S3-compatible store): content is an object in
 * {@code erp.core.files.s3.bucket} whose key ({@code storageRef}) follows the same layout as LOCAL,
 * {@code <tenantId>/<category>/<yyyy>/<MM>/<documentId>_<filename>}. Needs
 * {@code software.amazon.awssdk:s3} on the classpath (an optional dependency of erp-core); the
 * auto-configuration only creates it when that class is present.
 *
 * <p>{@link #publicUrl} is {@code <public-base-url>/<key>} when {@code erp.core.files.s3.public-base-url}
 * is set (bucket website or CDN in front of the bucket), so public documents are redirected there
 * instead of being streamed through the application. Objects are written without an ACL: making the
 * public prefix readable is the bucket's/CDN's configuration.
 */
@Slf4j
public class S3StorageProvider implements StorageProvider {

    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;
    private final Clock clock;

    public S3StorageProvider(S3Client s3, String bucket, String publicBaseUrl) {
        this(s3, bucket, publicBaseUrl, Clock.systemUTC());
    }

    public S3StorageProvider(S3Client s3, String bucket, String publicBaseUrl, Clock clock) {
        this.s3 = s3;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl == null || publicBaseUrl.isBlank() ? null : stripTrailingSlash(publicBaseUrl);
        this.clock = clock;
    }

    @Override
    public String key() {
        return StorageKeys.S3;
    }

    @Override
    public StoredObject put(StorageTarget target, InputStream in, long size, String contentType) {
        String objectKey = StoragePaths.relativeKey(target, clock);
        try (InputStream stream = in) {
            s3.putObject(PutObjectRequest.builder()
                    .bucket(bucket).key(objectKey).contentType(contentType).contentLength(size).build(),
                RequestBody.fromInputStream(stream, size));
            return new StoredObject(objectKey, size);
        } catch (SdkException | java.io.IOException e) {
            log.error("S3 put failed for {}/{}", bucket, objectKey, e);
            throw unavailable();
        }
    }

    @Override
    public InputStream get(String storageRef) {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(storageRef).build());
        } catch (SdkException e) {
            log.error("S3 get failed for {}/{}", bucket, storageRef, e);
            throw unavailable();
        }
    }

    @Override
    public void delete(String storageRef) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageRef).build());
        } catch (SdkException e) {
            log.warn("S3 delete failed for {}/{}", bucket, storageRef, e);
        }
    }

    @Override
    public Optional<String> publicUrl(String storageRef) {
        return publicBaseUrl == null ? Optional.empty() : Optional.of(publicBaseUrl + "/" + storageRef);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private LocalizedException unavailable() {
        return new LocalizedException(Status.INTERNAL_ERROR, FileErrorCodes.FILE_STORAGE_UNAVAILABLE, key());
    }
}
