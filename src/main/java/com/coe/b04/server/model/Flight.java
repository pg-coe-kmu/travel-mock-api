package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flight {
    private String flightId;

    private String airline;
    private String flightNumber;

    private String departureAirport;
    private String arrivalAirport;

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private String travelClass;

    private BigDecimal price;
    private String currency;

    private int availableSeats;
}
