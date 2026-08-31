package com.coe.b04.server.repository;

import com.coe.b04.server.enums.TravelClass;
import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.model.Flight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightRepositoryTest {

    private FlightRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FlightRepository();
        repository.setFlights(List.of(
                flight("FL-1", "BCN", "FCO", "2026-09-01T08:00:00", TravelClass.ECONOMY, "99.00", 10),
                flight("FL-2", "BCN", "FCO", "2026-09-02T08:00:00", TravelClass.BUSINESS, "199.00", 2),
                flight("FL-3", "MAD", "FCO", "2026-09-01T10:00:00", TravelClass.ECONOMY, "120.00", 5)
        ));
    }

    private Flight flight(String flightId, String departureAirport, String arrivalAirport,
                          String departureTime, TravelClass travelClass, String price, int availableSeats) {
        Flight flight = new Flight();
        flight.setFlightId(flightId);
        flight.setDepartureAirport(departureAirport);
        flight.setArrivalAirport(arrivalAirport);
        flight.setDepartureTime(LocalDateTime.parse(departureTime));
        flight.setTravelClass(travelClass);
        flight.setPrice(new BigDecimal(price));
        flight.setAvailableSeats(availableSeats);
        return flight;
    }

    private FlightRequest request(String origin, String destination, LocalDate departureDate,
                                  int adults, int children) {
        return FlightRequest.builder()
                .originIataCode(origin)
                .destinationIataCode(destination)
                .departureDate(departureDate)
                .numberOfAdults(adults)
                .numberOfChildren(children)
                .build();
    }

    @Test
    void filtersByDateAndAirportCodesCaseInsensitive() {
        List<Flight> result = repository.getFlightsByDate(
                request("bcn", "fco", LocalDate.of(2026, 9, 1), 1, 0));

        assertEquals(1, result.size());
        assertEquals("FL-1", result.getFirst().getFlightId());
    }

    @Test
    void filtersByAvailableSeats() {
        List<Flight> result = repository.getFlightsByDate(
                request("BCN", "FCO", LocalDate.of(2026, 9, 2), 2, 1));

        assertTrue(result.isEmpty());
    }

    @Test
    void filtersByTravelClass() {
        List<Flight> result = repository.getFlightsByDate(
                FlightRequest.builder()
                        .originIataCode("BCN")
                        .destinationIataCode("FCO")
                        .departureDate(LocalDate.of(2026, 9, 2))
                        .numberOfAdults(1)
                        .travelClass(TravelClass.BUSINESS)
                        .build());

        assertEquals(1, result.size());
        assertEquals("FL-2", result.getFirst().getFlightId());
    }

    @Test
    void filtersByMaxPrice() {
        List<Flight> result = repository.getFlightsByDate(
                FlightRequest.builder()
                        .originIataCode("MAD")
                        .destinationIataCode("FCO")
                        .departureDate(LocalDate.of(2026, 9, 1))
                        .numberOfAdults(1)
                        .maxPrice(110.0)
                        .build());

        assertTrue(result.isEmpty());
    }

    @Test
    void rangeFilterIsInclusive() {
        List<Flight> result = repository.getFlightsByRange(
                FlightRequest.builder()
                        .originIataCode("BCN")
                        .destinationIataCode("FCO")
                        .departureDateFrom(LocalDate.of(2026, 9, 1))
                        .departureDateTo(LocalDate.of(2026, 9, 2))
                        .numberOfAdults(1)
                        .build());

        assertEquals(2, result.size());
        assertEquals(List.of("FL-1", "FL-2"), result.stream().map(Flight::getFlightId).toList());
    }
}
