package com.coe.b04.server.model;

import com.coe.b04.server.enums.Direction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Flug-Snapshot, 1:1 zur Tabelle reservation_flights.
 * flightId ist die externe Mock-API-ID (z.B. FL-1001), ueber die
 * /details den vollen Flug aufloest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationFlightDetail {

    private Direction direction;
    private String flightId;
    private String airline;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private OffsetDateTime departureAt;
    private OffsetDateTime arrivalAt;
}
