package com.coe.b04.server.io;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * POST /reservations - referenziert das ausgewaehlte Angebot (IDs)
 * plus den Reise-Rahmen. Preise werden NICHT mitgeschickt, sondern
 * vom Backend aus den Mock-Daten geladen und eingefroren.
 * Ableitungen: checkIn = departureDate, checkOut = returnDate (bzw.
 * departureDate + 1 ohne Rueckreise), Pickup/Return analog.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {

    @NotBlank(message = "origin is required")
    private String origin;

    @NotBlank(message = "destination is required")
    private String destination;

    @NotNull(message = "departureDate is required")
    private LocalDate departureDate;

    private LocalDate returnDate;

    @NotNull(message = "adults is required")
    @Min(value = 1, message = "adults must be at least 1")
    private Integer adults;

    @Min(value = 0, message = "children cannot be negative")
    private Integer children;

    @Min(value = 0, message = "infants cannot be negative")
    private Integer infants;

    private String flightId;
    private String returnFlightId;
    private String hotelId;
    private String roomId;
    private String carId;
    private String providerId;

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code")
    private String currency;
}
