package com.coe.b04.server.model;

import com.coe.b04.server.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Eine reservierte Leistung, 1:1 zur Tabelle reservation_services.
 * Welche Felder gefuellt sind, haengt vom serviceType ab
 * (DB-Constraints chk_services_*_fields erzwingen das gleiche).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItem {

    private UUID id;
    private ServiceType serviceType;
    private String serviceId;
    private String providerId;

    private BigDecimal price;

    // nur HOTEL
    private String roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;

    // nur CAR
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private String pickupLocation;
    private String returnLocation;
}
