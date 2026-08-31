package com.coe.b04.server.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum TravelClass {
    ECONOMY("Economy"),
    PREMIUM_ECONOMY("Premium Economy"),
    BUSINESS("Business"),
    FIRST("First");

    private final String name;

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static TravelClass fromValue(String value) {
        return Arrays.stream(values())
                .filter(tc -> tc.getName().equalsIgnoreCase(value)
                        || tc.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown travel class: " + value));
    }
}
