package com.coe.b04.server.io;

import com.coe.b04.server.model.Flight;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class FlightResponse {
    private LocalDateTime timestamp;
    private int totalCount;
    private List<Flight> flights;

    public FlightResponse(List<Flight> flights) {
        this.timestamp = LocalDateTime.now();
        this.totalCount = flights != null ? flights.size() : 0;
        this.flights = flights;
    }
}
