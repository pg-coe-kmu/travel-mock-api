package com.coe.b04.server.enums;

/**
 * Leistungstyp einer Reservation (Spalte reservation_services.service_type).
 * Neue Leistungen (z.B. Versicherung) = neuer Enum-Wert, kein Schema-Umbau.
 */
public enum ServiceType {
    FLIGHT,
    RETURN_FLIGHT,
    HOTEL,
    CAR
}
