package com.erp.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * erp-core step 07 — the optional S3 provider against an in-memory fake {@link S3Client} (no network,
 * no credentials): object keys, round trip, delete, public URL and failure mapping.
 */
class S3StorageProviderTest {

    private static final Clock MARCH_2026 = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void put_get_delete_roundTripThroughTheClient_withTheSharedKeyLayout() throws IOException {
        FakeS3Client s3 = new FakeS3Client();
        S3StorageProvider provider = new S3StorageProvider(s3, "erp-files", null, MARCH_2026);
        byte[] content = "hello s3".getBytes(StandardCharsets.UTF_8);

        StoredObject stored = provider.put(new StorageTarget(3L, "PRODUCT_IMAGE", 11L, "shoe.png"),
            new ByteArrayInputStream(content), content.length, "image/png");

        assertThat(stored.storageRef()).isEqualTo("3/product_image/2026/03/11_shoe.png");
        assertThat(s3.objects).containsOnlyKeys("erp-files/3/product_image/2026/03/11_shoe.png");
        assertThat(s3.contentTypes).containsEntry("erp-files/3/product_image/2026/03/11_shoe.png", "image/png");
        try (InputStream in = provider.get(stored.storageRef())) {
            assertThat(in.readAllBytes()).isEqualTo(content);
        }
        assertThat(provider.key()).isEqualTo("S3");
        assertThat(provider.publicUrl(stored.storageRef())).isEmpty();

        provider.delete(stored.storageRef());
        assertThat(s3.objects).isEmpty();
    }

    @Test
    void publicUrl_isTheConfiguredBaseUrlPlusTheKey() {
        S3StorageProvider provider = new S3StorageProvider(new FakeS3Client(), "erp-files",
            "https://cdn.example.com/files/", MARCH_2026);

        assertThat(provider.publicUrl("3/product_image/2026/03/11_shoe.png"))
            .hasValue("https://cdn.example.com/files/3/product_image/2026/03/11_shoe.png");
    }

    @Test
    void clientFailures_becomeFileStorageUnavailable() {
        S3StorageProvider provider = new S3StorageProvider(new FakeS3Client(), "erp-files", null, MARCH_2026);

        assertThatThrownBy(() -> provider.get("no/such/key"))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo("FILE_STORAGE_UNAVAILABLE");
    }

    /** Minimal in-memory S3: only the three operations the provider uses. */
    static final class FakeS3Client implements S3Client {

        final Map<String, byte[]> objects = new HashMap<>();
        final Map<String, String> contentTypes = new HashMap<>();

        @Override
        public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
            try (InputStream in = body.contentStreamProvider().newStream()) {
                objects.put(request.bucket() + "/" + request.key(), in.readAllBytes());
                contentTypes.put(request.bucket() + "/" + request.key(), request.contentType());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return PutObjectResponse.builder().eTag("fake").build();
        }

        @Override
        public ResponseInputStream<GetObjectResponse> getObject(GetObjectRequest request) {
            byte[] content = objects.get(request.bucket() + "/" + request.key());
            if (content == null) {
                throw NoSuchKeyException.builder().message("no such key: " + request.key()).build();
            }
            return new ResponseInputStream<>(GetObjectResponse.builder().contentLength((long) content.length).build(),
                AbortableInputStream.create(new ByteArrayInputStream(content)));
        }

        @Override
        public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
            objects.remove(request.bucket() + "/" + request.key());
            return DeleteObjectResponse.builder().build();
        }

        @Override
        public String serviceName() {
            return "s3";
        }

        @Override
        public void close() {
        }
    }
}
