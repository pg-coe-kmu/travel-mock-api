package com.coe.b04.server.io;

import com.coe.b04.server.model.Hotel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class HotelResponse {
    private LocalDateTime timestamp;
    private int totalCount;
    private List<Hotel> hotels;

    public HotelResponse(List<Hotel> hotels) {
        this.timestamp = LocalDateTime.now();
        this.totalCount = hotels != null ? hotels.size() : 0;
        this.hotels = hotels;
    }
}
