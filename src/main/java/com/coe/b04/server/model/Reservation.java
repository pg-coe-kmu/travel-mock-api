package com.coe.b04.server.model;

import com.coe.b04.server.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reservation-Entitaet, 1:1 zur Tabelle reservations (db/reservations.sql).
 * Keine globalen Reisedaten mehr: Zeitraeume liegen auf Item-Ebene
 * (reservation_flights/hotels/cars). Preise sind Snapshots; Angebotsinhalte
 * werden ueber die externen IDs in den Detailtabellen aufgeloest.
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

    private int adults;
    private int children;
    private int infants;

    private String currency;
    private BigDecimal totalPrice;

    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime cancelledAt;

    private List<ReservationItem> items;
}
