package com.coe.b04.server.service;

import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.io.FlightResponse;
import com.coe.b04.server.repository.AirportRepository;
import com.coe.b04.server.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FlightServiceTest {

    private FlightService flightService;
    private FlightRepository flightRepository;
    private AirportRepository airportRepository;

    @BeforeEach
    void setUp() {
        flightRepository = mock(FlightRepository.class);
        airportRepository = mock(AirportRepository.class);
        flightService = new FlightService(flightRepository, airportRepository);
    }

    private FlightRequest singleDateRequest(int adults, int children, int infants) {
        return FlightRequest.builder()
                .origin("Barcelona")
                .destination("Rome")
                .departureDate(LocalDate.of(2026, 9, 1))
                .numberOfAdults(adults)
                .numberOfChildren(children)
                .numberOfInfants(infants)
                .build();
    }

    @Test
    void searchByDateResolvesAirportCodesAndDelegatesToRepository() {
        when(airportRepository.getAirportIataCodeByLocation("Barcelona")).thenReturn("BCN");
        when(airportRepository.getAirportIataCodeByLocation("Rome")).thenReturn("FCO");
        when(flightRepository.getFlightsByDate(any())).thenReturn(List.of());

        FlightResponse response = flightService.search(singleDateRequest(1, 0, 0));

        assertNotNull(response);
        ArgumentCaptor<FlightRequest> captor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightRepository).getFlightsByDate(captor.capture());
        assertEquals("BCN", captor.getValue().getOriginIataCode());
        assertEquals("FCO", captor.getValue().getDestinationIataCode());
    }

    @Test
    void searchByRangeDelegatesToRepository() {
        when(airportRepository.getAirportIataCodeByLocation(anyString())).thenReturn("BCN");
        when(flightRepository.getFlightsByRange(any())).thenReturn(List.of());

        FlightRequest request = FlightRequest.builder()
                .origin("Barcelona")
                .destination("Rome")
                .departureDateFrom(LocalDate.of(2026, 9, 1))
                .departureDateTo(LocalDate.of(2026, 9, 7))
                .numberOfAdults(1)
                .build();

        FlightResponse response = flightService.search(request);

        assertNotNull(response);
        verify(flightRepository).getFlightsByRange(any());
    }

    @Test
    void throwsWhenRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> flightService.search(null));
    }

    @Test
    void throwsWhenNoPassengers() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> flightService.search(singleDateRequest(0, 0, 0)));
        assertEquals("At least one adult or child must be specified", e.getMessage());
    }

    @Test
    void throwsWhenPassengerCountsAreNegative() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> flightService.search(singleDateRequest(-1, 0, 0)));
        assertEquals("Passenger counts cannot be negative", e.getMessage());
    }

    @Test
    void throwsWhenInfantsTravelWithoutAdult() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> flightService.search(singleDateRequest(0, 1, 1)));
        assertEquals("Infants cannot travel without an adult", e.getMessage());
    }

    @Test
    void throwsWhenMoreInfantsThanAdults() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> flightService.search(singleDateRequest(1, 0, 2)));
        assertEquals("Maximum 1 infant per adult allowed", e.getMessage());
    }

    @Test
    void throwsWhenDateRangeIsInvalid() {
        FlightRequest request = FlightRequest.builder()
                .origin("Barcelona")
                .destination("Rome")
                .departureDateFrom(LocalDate.of(2026, 9, 10))
                .departureDateTo(LocalDate.of(2026, 9, 1))
                .numberOfAdults(1)
                .build();

        RuntimeException e = assertThrows(RuntimeException.class, () -> flightService.search(request));
        assertEquals("departureDateFrom must be before or equal to departureDateTo", e.getMessage());
    }

    @Test
    void throwsWhenNoDatesProvided() {
        FlightRequest request = FlightRequest.builder()
                .origin("Barcelona")
                .destination("Rome")
                .numberOfAdults(1)
                .build();

        assertThrows(IllegalArgumentException.class, () -> flightService.search(request));
    }

    @Test
    void throwsWhenAirportNotFound() {
        when(airportRepository.getAirportIataCodeByLocation(anyString())).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> flightService.search(singleDateRequest(1, 0, 0)));
    }
}
