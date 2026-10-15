package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Booking-Entitaet, 1:1 zur Tabelle bookings (db/bookings.sql).
 * reservationNumber wird per Join aus reservations mitgelesen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    private UUID id;
    private String bookingNumber;
    private UUID reservationId;
    private String reservationNumber;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime cancelledAt;
}
