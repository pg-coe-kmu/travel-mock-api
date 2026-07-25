package com.coe.b04.server.service;

import com.coe.b04.server.model.Flight;
import com.coe.b04.server.repository.AirportRepository;
import com.coe.b04.server.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;

    public FlightService(FlightRepository flightRepository, AirportRepository airportRepository) {
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
    }

    public List<Flight> searchFlights(String from, String destination, LocalDate departureDate, String travelClass) {
        // Get the airport codes for the given locations
        String departureAirportCode = getAirportCodeByLocation(from);
        String arrivalAirportCode = getAirportCodeByLocation(destination);

        return flightRepository.searchFlights(departureAirportCode, arrivalAirportCode, departureDate, travelClass);
    }

    private String getAirportCodeByLocation(String location) {
        String airportCode = airportRepository.getAirportIataCodeByLocation(location);
        if (airportCode == null) {
            throw new RuntimeException("No airport found for location: " + location);
        }
        return airportCode;
    }
}
