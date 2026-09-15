package com.coe.b04.server.model;

import com.coe.b04.server.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Generische Leistung einer Reservation, 1:1 zur Tabelle reservation_items.
 * Enthaelt NUR gemeinsame Felder; service-spezifische Daten (inkl. der
 * externen Mock-API-IDs) liegen in genau einer der Detailtabellen und
 * damit in genau einem der Detailfelder (flight | hotel | car).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItem {

    private UUID id;
    private ServiceType itemType;
    private BigDecimal price;

    private ReservationFlightDetail flight;
    private ReservationHotelDetail hotel;
    private ReservationCarDetail car;
}
