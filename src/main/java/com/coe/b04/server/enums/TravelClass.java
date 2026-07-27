package com.coe.b04.server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

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
}
