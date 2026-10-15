package com.coe.b04.server.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * GET /booking/snapshot - Buchungskopf plus eingebetteter
 * Reservation-Snapshot (trip, items, price; Status PAID bzw.
 * CANCELLED nach Stornierung).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {

    private String bookingNumber;
    private String reservationNumber;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime cancelledAt;

    private ReservationResponse reservation;
}
