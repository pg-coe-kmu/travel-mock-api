package com.coe.b04.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileEntry {
    AIRPORTS_FILE("airports.json"),
    HOTELS_FILE("hotels.json"),
    FLIGHTS_FILE("flights.json"),
    CARS_FILE("cars.json");

    private final String filename;
}
