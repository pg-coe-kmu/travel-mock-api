package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Mietwagen-Snapshot, 1:1 zur Tabelle reservation_cars.
 * carId/providerId sind die externen Mock-API-IDs (z.B. CAR-1001 /
 * PROV-SIXT-01), ueber die /details das volle Angebot aufloest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCarDetail {

    private String carId;
    private String providerId;
    private OffsetDateTime pickupAt;
    private OffsetDateTime returnAt;
    private String pickupLocation;
    private String returnLocation;
    private String vehicleName;
}
