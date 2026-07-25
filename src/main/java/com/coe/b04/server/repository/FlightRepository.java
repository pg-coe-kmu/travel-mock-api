package com.coe.b04.server.repository;

import com.coe.b04.server.model.Flight;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Repository
public class FlightRepository {
    private List<Flight> flights;

    public List<Flight> searchFlights(String from, String arrival, LocalDate departureDate, String travelClass) {
        return flights.stream()
                .filter(flight -> flight.getDepartureAirport().equalsIgnoreCase(from))
                .filter(flight -> flight.getArrivalAirport().equalsIgnoreCase(arrival))
                .filter(flight -> flight.getDepartureTime().toLocalDate().equals(departureDate))
                .filter(flight -> travelClass == null || flight.getTravelClass().equalsIgnoreCase(travelClass))
                .toList();
    }
}
