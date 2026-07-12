package com.coe.b04.server.utils;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public final class Constants {
    public static final Path DEFAULT_MOCK_DATA_PATH = Paths.get("data", "mock");
    public static final Path MOCK_DATA_PATH = Paths.get(
            System.getenv().getOrDefault("MOCK_DATA_REMOTE_PATH", DEFAULT_MOCK_DATA_PATH.toString())
    );

    public static final Path HOTELS_PATH = MOCK_DATA_PATH.resolve("hotels.json");
    public static final Path FLIGHTS_PATH = MOCK_DATA_PATH.resolve("flights.json");
    public static final Path CARS_PATH = MOCK_DATA_PATH.resolve("cars.json");
}


