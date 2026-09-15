package com.coe.b04.server.io;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import com.coe.b04.server.model.ReservationCarDetail;
import com.coe.b04.server.model.ReservationFlightDetail;
import com.coe.b04.server.model.ReservationHotelDetail;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /reservation/snapshot - Snapshot-Daten aus der DB.
 * Neue flexible Struktur: generische items, jedes mit type + price und
 * genau einem Detailobjekt (flight | hotel | car). Keine fachfremden
 * null-Felder (NON_NULL). Keine globalen Reisedaten im trip - die
 * Zeitraeume liegen in den Detailobjekten der Items.
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
    private List<ItemResponse> items;
    private Price price;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trip {
        private String origin;
        private String destination;
        private int adults;
        private int children;
        private int infants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItemResponse {
        private ServiceType type;
        private BigDecimal price;
        private ReservationFlightDetail flight;
        private ReservationHotelDetail hotel;
        private ReservationCarDetail car;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Price {
        private BigDecimal totalPrice;
        private String currency;
    }
}
