package com.cloudnest.service.storage;

import com.cloudnest.dto.ProviderCapabilities;
import com.cloudnest.dto.ProviderHealth;
import com.cloudnest.dto.ProviderQuota;
import com.cloudnest.entity.CloudProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

/** Backblaze B2's S3-compatible API implementation. */
@Slf4j
@Component
public class BackblazeB2StorageProvider implements CloudStorageProvider {
    @Value("${app.backblaze.endpoint}") private String endpoint;
    @Value("${app.backblaze.region}") private String region;
    @Value("${app.backblaze.access-key-id:}") private String configuredKeyId;
    @Value("${app.backblaze.secret-access-key:}") private String configuredApplicationKey;
    @Value("${app.backblaze.bucket-name:}") private String defaultBucket;
    @Value("${app.quotas.backblaze-bytes:1073741824}") private long quotaBytes;

    private String bucket(CloudProvider provider) {
        String value = provider.getBucketName();
        if (value == null || value.isBlank()) value = defaultBucket;
        if (value == null || value.isBlank()) throw new IllegalStateException("Backblaze bucket name is required.");
        return value;
    }

    private S3Client client(CloudProvider provider) {
        String keyId = provider.getAccessKeyId();
        String applicationKey = provider.getAccessToken();
        if (keyId == null || keyId.isBlank()) keyId = configuredKeyId;
        if (applicationKey == null || applicationKey.isBlank()) applicationKey = configuredApplicationKey;
        if (keyId == null || keyId.isBlank() || applicationKey == null || applicationKey.isBlank()) {
            throw new IllegalStateException("Backblaze Key ID and Application Key are required.");
        }
        return S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey)))
                .httpClient(UrlConnectionHttpClient.create()).forcePathStyle(true).build();
    }

    private String key(CloudProvider provider, String fileName) { return "user-" + provider.getUser().getId() + "/" + fileName; }

    @Override public String upload(CloudProvider provider, File file, String fileName) {
        String objectKey = key(provider, fileName);
        try (S3Client s3 = client(provider)) {
            s3.putObject(PutObjectRequest.builder().bucket(bucket(provider)).key(objectKey).contentType("application/octet-stream").build(), RequestBody.fromFile(file));
            return objectKey;
        } catch (Exception e) { throw new RuntimeException("Backblaze upload failed: " + e.getMessage(), e); }
    }

    @Override public File download(CloudProvider provider, String storagePath) {
        try {
            File result = File.createTempFile("cloudnest-restore-", ".tmp");
            try (S3Client s3 = client(provider)) {
                s3.getObject(GetObjectRequest.builder().bucket(bucket(provider)).key(storagePath).build(), ResponseTransformer.toFile(Paths.get(result.getAbsolutePath())));
            }
            return result;
        } catch (Exception e) { throw new RuntimeException("Backblaze download failed: " + e.getMessage(), e); }
    }

    @Override public void delete(CloudProvider provider, String storagePath) {
        try (S3Client s3 = client(provider)) { s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket(provider)).key(storagePath).build()); }
        catch (Exception e) { log.warn("Backblaze delete failed for {}: {}", storagePath, e.getMessage()); }
    }

    @Override public long getUsedStorageBytes(CloudProvider provider) {
        long total = 0;
        try (S3Client s3 = client(provider)) {
            var request = ListObjectsV2Request.builder().bucket(bucket(provider)).prefix("user-" + provider.getUser().getId() + "/").build();
            while (true) {
                var response = s3.listObjectsV2(request);
                total += response.contents().stream().mapToLong(item -> item.size() == null ? 0 : item.size()).sum();
                if (!response.isTruncated()) return total;
                request = request.toBuilder().continuationToken(response.nextContinuationToken()).build();
            }
        } catch (Exception e) { log.warn("Could not get Backblaze usage: {}", e.getMessage()); return 0; }
    }

    @Override public ProviderHealth healthCheck(CloudProvider provider) {
        try (S3Client s3 = client(provider)) {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket(provider)).build());
            return ProviderHealth.connected("Backblaze bucket '" + bucket(provider) + "' reachable");
        } catch (Exception e) { return ProviderHealth.unreachable("Backblaze unreachable: " + e.getMessage()); }
    }

    @Override public ProviderQuota getQuota(CloudProvider provider) { return new ProviderQuota(getUsedStorageBytes(provider), quotaBytes); }
    @Override public ProviderCapabilities getCapabilities() { return new ProviderCapabilities(false, true, true); }
}
