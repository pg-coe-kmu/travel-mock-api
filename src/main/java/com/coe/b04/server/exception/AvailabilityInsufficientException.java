package com.coe.b04.server.exception;

/*
 * Wird geworfen, wenn beim Anlegen einer Reservation die Katalog-
 * Availability fuer ein Item nicht (mehr) ausreicht. Die Reservation-
 * Transaktion rollt dann vollstaendig zurueck; der ReservationService
 * uebersetzt die Exception in HTTP 409.
 */
public class AvailabilityInsufficientException extends RuntimeException {

    public AvailabilityInsufficientException(String catalogTable, String externalId) {
        super("Insufficient availability in " + catalogTable + " for external_id " + externalId
                + " (or catalog row missing)");
    }
}
