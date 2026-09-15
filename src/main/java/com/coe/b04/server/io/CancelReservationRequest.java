package com.coe.b04.server.io;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /reservation/cancel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelReservationRequest {

    @NotBlank(message = "reservationNumber is required")
    private String reservationNumber;
}
