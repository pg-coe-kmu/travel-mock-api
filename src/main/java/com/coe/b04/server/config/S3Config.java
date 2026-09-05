package com.coe.b04.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/*
 * Creates the S3 client for the Supabase S3-compatible storage API.
 * The values come from the .env file (local profile) or from environment
 * variables (remote profile / CI secrets).
 *
 * Security: the secrets are only passed as method parameters here and are
 * never stored in fields or exposed to other beans - EnvConfig holds no
 * secrets. The only thing exposed is the built S3Client.
 *
 * Supabase requires path-style access and S3 access keys generated in the
 * dashboard (Storage -> Settings -> S3 Access Keys).
 */

@Configuration
public class S3Config {

    /*
     * Lazy: the client is only created on first actual use, so the local
     * profile starts without S3 configuration.
     */
    @Bean
    @Lazy
    public S3Client s3Client(@Value("${supabase.s3.endpoint:}") String endpoint,
                             @Value("${supabase.s3.region:eu-central-1}") String region,
                             @Value("${supabase.s3.accessKey:}") String accessKey,
                             @Value("${supabase.s3.secretKey:}") String secretKey) {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "Supabase S3 is not configured: set the SUPABASE_S3_ENDPOINT, SUPABASE_S3_ACCESS_KEY "
                            + "and SUPABASE_S3_SECRET_KEY environment variables.");
        }

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }
}
