package com.coe.b04.server.reader;

import com.coe.b04.server.config.EnvConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

/*
 * Adapter for the Supabase S3-compatible storage API: fetches the mock data
 * files from the bucket and deserializes them. Used by the Loader.
 */
@Profile("remote")
@Slf4j
@Component
public class S3Reader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client;

    public S3Reader(@Lazy S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public <T> List<T> load(String fileName, Class<T[]> clazz) {
        String bucket = EnvConfig.getS3Bucket();
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException(
                    "Supabase S3 bucket is not configured: set the SUPABASE_BUCKET environment variable.");
        }

        String folder = EnvConfig.getS3Folder();
        String objectPath = StringUtils.hasText(folder) ? folder + "/" + fileName : fileName;
        try {
            log.info("Load {} from S3 bucket {}.", fileName, objectPath);
            byte[] body = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectPath)
                    .build()).asByteArray();
            return Arrays.asList(objectMapper.readValue(body, clazz));
        } catch (SdkException e) {
            throw new IllegalStateException(
                    "Failed to load mock data from S3 bucket '" + bucket + "' (" + objectPath + ")", e);
        }
    }
}
