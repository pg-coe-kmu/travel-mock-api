package com.coe.b04.server.reader;

import com.coe.b04.server.config.EnvConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/*
 * Reads the mock data files from the local data folder
 * (EnvConfig.getLocalDataPath(), default /data). Used by the Loader.
 */
@Profile("local")
@Slf4j
@Component
public class LocalReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> List<T> load(String fileName, Class<T[]> clazz) {
        Path path = EnvConfig.getLocalDataPath().resolve(fileName);
        log.info("Load {} from {}.", fileName, path);

        try (InputStream is = Files.newInputStream(path)) {
            return Arrays.asList(objectMapper.readValue(is, clazz));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock data from " + path, e);
        }
    }
}
