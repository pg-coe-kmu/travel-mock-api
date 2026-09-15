package com.coe.b04.server.enums;

/**
 * Leistungstyp eines Reservation-Items (Spalte reservation_items.item_type).
 * Flugrichtung ist KEIN Typ mehr - sie steht in reservation_flights.direction.
 * Neue Leistungen (z.B. TRAIN, TRANSFER, ACTIVITY) = neuer Enum-Wert
 * plus eigene Detailtabelle, kein Umbau der generischen Items.
 */
public enum ServiceType {
    FLIGHT,
    HOTEL,
    CAR
}
