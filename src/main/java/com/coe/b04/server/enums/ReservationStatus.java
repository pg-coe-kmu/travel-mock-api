package com.coe.b04.server.enums;

/**
 * Lebenszyklus einer Reservation.
 * PAID existiert bewusst nicht: eine bezahlte Reservation wird spaeter
 * zu einer Booking (eigener Prozess) und ist damit kein Reservation-Status.
 * EXPIRED wird lazy bei Zugriff gesetzt, sobald expiresAt erreicht ist.
 */
public enum ReservationStatus {
    PENDING,
    EXPIRED,
    CANCELLED
}
