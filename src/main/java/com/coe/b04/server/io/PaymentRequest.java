package com.coe.b04.server.io;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /payment - triggert die Zahlung einer Reservation.
 * success=true: Reservation wird (innerhalb der 30 Minuten) zur Booking.
 * success=false: Zahlung fehlgeschlagen, Reservation bleibt unveraendert.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "reservationNumber is required")
    private String reservationNumber;

    @NotNull(message = "success is required")
    private Boolean success;
}
