package com.coe.b04.server.enums;

/**
 * Flugrichtung (Spalte reservation_flights.direction).
 * Hin- UND Rueckflug sind zwei separate Items; ein RETURN ohne
 * OUTBOUND ist auf DB-Ebene moeglich (App validiert).
 */
public enum Direction {
    OUTBOUND,
    RETURN
}
