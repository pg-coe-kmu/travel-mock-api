package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {
    private String roomId;
    private String roomType;
    private String board;

    private BigDecimal pricePerNight;
    private int availableRooms;

    private MaxOccupancy maxOccupancy;

    private String bedType;
    private int roomSizeSqm;

    private CancellationPolicy cancellationPolicy;

    private List<String> roomAmenities;
}
