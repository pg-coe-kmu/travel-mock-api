package com.coe.b04.server.enums;

/**
 * Lebenszyklus einer Reservation.
 * PAID wird beim erfolgreichen Payment gesetzt (PENDING -> PAID); dabei
 * entsteht die Booking mit eigener Buchungsnummer. EXPIRED wird lazy bei
 * Zugriff gesetzt, sobald expiresAt erreicht ist.
 */
public enum ReservationStatus {
    PENDING,
    EXPIRED,
    CANCELLED,
    PAID
}
