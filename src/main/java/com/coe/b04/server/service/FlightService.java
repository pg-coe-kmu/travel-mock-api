package com.coe.b04.server.service;

import com.coe.b04.server.enums.TravelClass;
import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.io.FlightResponse;
import com.coe.b04.server.repository.AirportRepository;
import com.coe.b04.server.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;

    public FlightService(FlightRepository flightRepository, AirportRepository airportRepository) {
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
    }

    public FlightResponse search(FlightRequest flightRequest) {
        if(flightRequest == null)
            throw new IllegalArgumentException("flightRequest cannot be null");

        try {
            // Check valid passenger combination
            isValidPassengerCombination(flightRequest.getNumberOfAdults(), flightRequest.getNumberOfChildren(), flightRequest.getNumberOfInfants());

            // Check valid dates
            if(flightRequest.getDepartureDateFrom() != null && flightRequest.getDepartureDateTo() != null)
                isValidDateRange(flightRequest.getDepartureDateFrom(), flightRequest.getDepartureDateTo());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if(flightRequest.getDepartureDateFrom() != null && flightRequest.getDepartureDateTo() != null)
            return searchFlightsByRange(flightRequest.getOrigin(), flightRequest.getDestination(), flightRequest.getDepartureDateFrom(), flightRequest.getDepartureDateTo(), flightRequest.getNumberOfAdults(), flightRequest.getNumberOfChildren(), flightRequest.getNumberOfInfants(), flightRequest.getTravelClass(), flightRequest.getMaxPrice());

        if(flightRequest.getDepartureDate() != null)
            return searchFlightsByDate(flightRequest.getOrigin(), flightRequest.getDestination(), flightRequest.getDepartureDate(), flightRequest.getNumberOfAdults(), flightRequest.getNumberOfChildren(), flightRequest.getNumberOfInfants(), flightRequest.getTravelClass(), flightRequest.getMaxPrice());

        // Fallback: No Date
        throw new IllegalArgumentException("Either departureDate or (departureDateFrom and departureDateTo) must be provided");
    }

    private FlightResponse searchFlightsByDate(String origin, String destination, LocalDate departureDate, Integer numberOfAdults, Integer numberOfChildren, Integer numberOfInfants, TravelClass travelClass, Double maxPrice) {
        FlightRequest flightRequest = FlightRequest.builder()
                .originIataCode(getAirportCodeByLocation(origin))
                .destinationIataCode(getAirportCodeByLocation(destination))
                .departureDate(departureDate)
                .numberOfAdults(numberOfAdults)
                .numberOfChildren(numberOfChildren)
                .numberOfInfants(numberOfInfants)
                .travelClass(travelClass)
                .maxPrice(maxPrice)
                .build();

        return new FlightResponse(flightRepository.getFlightsByDate(flightRequest));
    }

    private FlightResponse searchFlightsByRange(String origin, String destination, LocalDate departureDateFrom, LocalDate departureDateTo, Integer numberOfAdults, Integer numberOfChildren, Integer numberOfInfants, TravelClass travelClass, Double maxPrice) {
        FlightRequest flightRequest = FlightRequest.builder()
                .originIataCode(getAirportCodeByLocation(origin))
                .destinationIataCode(getAirportCodeByLocation(destination))
                .departureDateFrom(departureDateFrom)
                .departureDateTo(departureDateTo)
                .numberOfAdults(numberOfAdults)
                .numberOfChildren(numberOfChildren)
                .numberOfInfants(numberOfInfants)
                .travelClass(travelClass)
                .maxPrice(maxPrice)
                .build();

        return new FlightResponse(flightRepository.getFlightsByRange(flightRequest));
    }


    /**
     * Retrieves the airport code for a given location.
     *
     * @param location the location to search for an airport code
     * @return the airport code corresponding to the location
     * @throws IllegalArgumentException if no airport is found for the given location
     */
    private String getAirportCodeByLocation(String location) {
        String airportCode = airportRepository.getAirportIataCodeByLocation(location);
        if (airportCode == null)
            throw new IllegalArgumentException("No airport found for location: " + location);

        return airportCode;
    }

    /**
     * Validates the passenger combination for a flight booking.
     *
     * @param numberOfAdults     number of adult passengers (required, cannot be null)
     * @param numberOfChildren   number of child passengers (optional, defaults to 0 if null)
     * @param numberOfInfants    number of infant passengers (optional, defaults to 0 if null)
     * @throws IllegalArgumentException if the passenger combination is invalid
     */
    private void isValidPassengerCombination(Integer numberOfAdults, Integer numberOfChildren, Integer numberOfInfants) {
        // Rule 1: No negative passenger counts allowed
        if (numberOfAdults < 0 || numberOfChildren < 0 || numberOfInfants < 0) {
            throw new IllegalArgumentException("Passenger counts cannot be negative");
        }

        // Rule 2: At least one adult or child must be specified
        if (numberOfAdults == 0 && numberOfChildren == 0) {
            throw new IllegalArgumentException("At least one adult or child must be specified");
        }

        // Infant-specific rules (only apply if infants are present)
        if (numberOfInfants > 0) {
            // Rule 3: Infants cannot travel without an adult
            if (numberOfAdults == 0) {
                throw new IllegalArgumentException("Infants cannot travel without an adult");
            }
            // Rule 4: Maximum 1 infant per adult (infant sits on adult's lap)
            if (numberOfInfants > numberOfAdults) {
                throw new IllegalArgumentException("Maximum 1 infant per adult allowed");
            }
        }
    }

    /**
     * Validates the date range for a flight booking.
     *
     * @param departureDateFrom     the start date of the search range (inclusive)
     * @param departureDateTo       the end date of the search range (inclusive)
     * @throws IllegalArgumentException if the date range is invalid
     */
    private void isValidDateRange(LocalDate departureDateFrom, LocalDate departureDateTo) {
        if (departureDateFrom.isAfter(departureDateTo)) {
            throw new IllegalArgumentException("departureDateFrom must be before or equal to departureDateTo");
        }
    }
}
