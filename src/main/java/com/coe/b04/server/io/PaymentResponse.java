package com.coe.b04.server.io;

import com.coe.b04.server.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Antwort auf POST /payment: die aus der bezahlten Reservation
 * entstandene Booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String bookingNumber;
    private String reservationNumber;
    private ReservationStatus status;
    private OffsetDateTime paidAt;
    private BigDecimal totalPrice;
    private String currency;
}
