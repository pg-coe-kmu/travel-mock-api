package com.coe.b04.server.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * GET /booking/snapshot - Buchungskopf plus eingebetteter
 * Reservation-Snapshot (trip, items, price; Status PAID).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String bookingNumber;
    private String reservationNumber;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;

    private ReservationResponse reservation;
}
