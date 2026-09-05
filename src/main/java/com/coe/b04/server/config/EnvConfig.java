package com.coe.b04.server.config;

import lombok.Getter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/*
 * Static access to the non-secret environment values (.env or environment
 * variables). Secrets (S3 access keys) are handled exclusively inside
 * S3Config and are never exposed here.
 */
@Component
public class EnvConfig {

    @Getter
    private static Path localDataPath;
    @Getter
    private static String s3Bucket;
    @Getter
    private static String s3Folder;

    public EnvConfig(Environment environment) {
        EnvConfig.localDataPath = Path.of(environment.getProperty("LOCAL_DATA_PATH", "data"));
        EnvConfig.s3Bucket = environment.getProperty("supabase.s3.bucket", "");
        EnvConfig.s3Folder = environment.getProperty("supabase.s3.folder", "");
    }
}
