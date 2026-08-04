package com.coe.b04.server.repository;

import com.coe.b04.server.enums.TravelClass;
import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.model.Flight;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.List;

import static java.lang.Integer.sum;

@Getter
@Setter
@Repository
public class FlightRepository {
    private List<Flight> flights;

    public List<Flight> getFlightsByDate(FlightRequest flightRequest) {
        return getFlightsByDefaultParams(
                flightRequest.getOriginIataCode(),
                flightRequest.getDestinationIataCode(),
                flightRequest.getNumberOfAdults(),
                flightRequest.getNumberOfChildren(),
                flightRequest.getTravelClass(),
                flightRequest.getMaxPrice())
                .stream()
                .filter(flight -> flight.getDepartureTime().toLocalDate().equals(flightRequest.getDepartureDate()))
                .toList();
    }

    public List<Flight> getFlightsByRange(FlightRequest flightRequest) {
        return getFlightsByDefaultParams(
                flightRequest.getOriginIataCode(),
                flightRequest.getDestinationIataCode(),
                flightRequest.getNumberOfAdults(),
                flightRequest.getNumberOfChildren(),
                flightRequest.getTravelClass(),
                flightRequest.getMaxPrice())
                .stream()
                .filter(flight -> !flight.getDepartureTime().toLocalDate().isBefore(flightRequest.getDepartureDateFrom()))
                .filter(flight -> !flight.getDepartureTime().toLocalDate().isAfter(flightRequest.getDepartureDateTo()))
                .toList();
    }

    private List<Flight> getFlightsByDefaultParams(String departureAirportCode, String arrivalAirportCode, Integer numberOfAdults, Integer numberOfChildren, TravelClass travelClass, Double maxPrice) {
        return flights.stream()
                .filter(flight -> flight.getDepartureAirport().equalsIgnoreCase(departureAirportCode))
                .filter(flight -> flight.getArrivalAirport().equalsIgnoreCase(arrivalAirportCode))
                .filter(flight -> flight.getAvailableSeats() >= sum(numberOfAdults, numberOfChildren))
                .filter(flight -> travelClass == null || flight.getTravelClass() == travelClass)
                .filter(flight -> maxPrice == null || flight.getPrice().doubleValue() <= maxPrice)
                .toList();
    }
}