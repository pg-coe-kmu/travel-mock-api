package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {
    private String hotelId;
    private String name;
    private String city;
    private String country;
    private String address;

    private int stars;

    private Rating rating;

    private List<String> hotelAmenities;

    private String checkInTime;
    private String checkOutTime;

    private String baseCurrency;

    private List<RoomType> roomTypes;
}
