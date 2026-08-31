package com.coe.b04.server.controller;

import com.coe.b04.server.enums.TravelClass;
import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.io.FlightResponse;
import com.coe.b04.server.service.FlightService;
import com.coe.b04.server.utils.TravelClassConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FlightControllerTest {

    private MockMvc mockMvc;
    private FlightService flightService;

    @BeforeEach
    void setUp() {
        flightService = mock(FlightService.class);

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new TravelClassConverter());

        mockMvc = MockMvcBuilders.standaloneSetup(new FlightController(flightService))
                .setConversionService(conversionService)
                .build();
    }

    @Test
    void searchFlightsBindsTravelClassFromDisplayValue() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .param("travelClass", "Business")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> requestCaptor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(requestCaptor.capture());
        assertEquals(TravelClass.BUSINESS, requestCaptor.getValue().getTravelClass());
    }

    @Test
    void shouldBindTravelClassFromEnumName() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .param("travelClass", "BUSINESS"))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> captor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(captor.capture());

        assertEquals(TravelClass.BUSINESS, captor.getValue().getTravelClass());
    }

    @Test
    void shouldReturnBadRequestForInvalidTravelClass() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .param("travelClass", "INVALID"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void shouldAllowMissingTravelClass() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> captor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(captor.capture());

        assertNull(captor.getValue().getTravelClass());
    }

    @Test
    void shouldReturnBadRequestWhenOriginMissing() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void shouldReturnBadRequestForNegativePassengerCounts() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void bindsDatesAndMaxPrice() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "2")
                        .param("numberOfChildren", "1")
                        .param("maxPrice", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<FlightRequest> captor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).search(captor.capture());
        FlightRequest request = captor.getValue();
        assertEquals(LocalDate.of(2026, 8, 1), request.getDepartureDate());
        assertEquals(2, request.getNumberOfAdults());
        assertEquals(1, request.getNumberOfChildren());
        assertEquals(0, request.getNumberOfInfants());
        assertEquals(500.0, request.getMaxPrice());
    }

    @Test
    void shouldReturnBadRequestForInvalidDate() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "not-a-date")
                        .param("numberOfAdults", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void returnsJsonResponseWithCount() throws Exception {
        when(flightService.search(any())).thenReturn(new FlightResponse(List.of()));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome")
                        .param("departureDate", "2026-08-01")
                        .param("numberOfAdults", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.flights").isEmpty());
    }
}
