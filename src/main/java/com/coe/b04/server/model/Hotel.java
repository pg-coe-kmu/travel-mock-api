package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {
    private String hotelId;
    private String name;
    private String city;
    private String country;

    private int stars;

    private String roomType;
    private String board;

    private BigDecimal pricePerNight;
    private String currency;

    private int availableRooms;

    private String checkInTime;
    private String checkOutTime;
}
