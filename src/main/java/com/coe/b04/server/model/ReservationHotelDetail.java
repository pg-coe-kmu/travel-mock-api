package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Hotel-Snapshot, 1:1 zur Tabelle reservation_hotels.
 * hotelId/roomId sind die externen Mock-API-IDs (z.B. HOT-1001 / ROOM-102);
 * hotel_name/room_name bleiben auch erhalten, wenn sich die Mock-API aendert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationHotelDetail {

    private String hotelId;
    private String roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String hotelName;
    private String roomName;
}
