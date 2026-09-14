package com.coe.b04.server.io;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /reservations/{reservationNumber} - Snapshot-Daten aus der DB,
 * ohne die vollen Hotel-/Flug-/Car-Inhalte (dafuer gibt es /details).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private String reservationNumber;
    private ReservationStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private long expiresInSeconds;

    private Trip trip;
    private List<ServiceItemResponse> services;
    private Price price;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trip {
        private String origin;
        private String destination;
        private LocalDate departureDate;
        private LocalDate returnDate;
        private int adults;
        private int children;
        private int infants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceItemResponse {
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Price {
        private BigDecimal totalPrice;
        private String currency;
        // null = Leistung nicht Teil der Reservation
        private BigDecimal flightPrice;
        private BigDecimal hotelPrice;
        private BigDecimal carPrice;
    }
}
