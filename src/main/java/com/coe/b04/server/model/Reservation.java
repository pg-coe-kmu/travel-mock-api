package com.coe.b04.server.model;

import com.coe.b04.server.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reservation-Entitaet, 1:1 zur Tabelle reservations (db/reservations.sql).
 * Preise/Zeiten sind Snapshots; Hotel-/Flug-/Car-Inhalte werden ueber die
 * IDs in reservation_services aufgeloest.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    private UUID id;
    private String reservationNumber;
    private ReservationStatus status;

    private String origin;
    private String destination;
    private LocalDate departureDate;
    private LocalDate returnDate;

    private int adults;
    private int children;
    private int infants;

    private String currency;
    private BigDecimal totalPrice;

    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime cancelledAt;

    private List<ReservationItem> services;
}
