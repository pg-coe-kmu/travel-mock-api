package com.coe.b04.server.controller;

import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.model.RoomType;
import com.coe.b04.server.service.CarService;
import com.coe.b04.server.service.FlightService;
import com.coe.b04.server.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DetailsControllerTest {

    private MockMvc mockMvc;
    private CarService carService;
    private HotelService hotelService;
    private FlightService flightService;

    @BeforeEach
    void setUp() {
        carService = mock(CarService.class);
        hotelService = mock(HotelService.class);
        flightService = mock(FlightService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DetailsController(carService, hotelService, flightService)).build();
    }

    @Test
    void carDetailsReturnsProviderWithMatchingCar() throws Exception {
        when(carService.getDetails(anyString(), anyString())).thenReturn(CarProvider.builder()
                .providerId("PROV-SIXT")
                .cars(List.of(Car.builder().carId("CAR-1").build()))
                .build());

        mockMvc.perform(get("/details/car")
                        .param("providerId", "PROV-SIXT")
                        .param("carId", "CAR-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("PROV-SIXT"))
                .andExpect(jsonPath("$.cars[0].carId").value("CAR-1"));

        verify(carService).getDetails("PROV-SIXT", "CAR-1");
    }

    @Test
    void carDetailsReturnsBadRequestWhenProviderIdMissing() throws Exception {
        mockMvc.perform(get("/details/car")
                        .param("carId", "CAR-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void carDetailsReturnsFullProviderWhenCarIdOmitted() throws Exception {
        when(carService.getDetails(anyString(), any())).thenReturn(CarProvider.builder()
                .providerId("PROV-SIXT")
                .cars(List.of(Car.builder().carId("CAR-1").build(), Car.builder().carId("CAR-2").build()))
                .build());

        mockMvc.perform(get("/details/car")
                        .param("providerId", "PROV-SIXT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("PROV-SIXT"))
                .andExpect(jsonPath("$.cars.length()").value(2));

        verify(carService).getDetails("PROV-SIXT", null);
    }

    @Test
    void carDetailsReturnsNotFoundWhenServiceThrows() throws Exception {
        when(carService.getDetails(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mockMvc.perform(get("/details/car")
                        .param("providerId", "PROV-UNKNOWN")
                        .param("carId", "CAR-UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hotelDetailsReturnsHotelWithMatchingRoom() throws Exception {
        RoomType room = new RoomType();
        room.setRoomId("ROOM-1");
        when(hotelService.getDetails(anyString(), anyString())).thenReturn(Hotel.builder()
                .hotelId("HOT-1")
                .roomTypes(List.of(room))
                .build());

        mockMvc.perform(get("/details/hotel")
                        .param("hotelId", "HOT-1")
                        .param("roomId", "ROOM-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value("HOT-1"))
                .andExpect(jsonPath("$.roomTypes[0].roomId").value("ROOM-1"));

        verify(hotelService).getDetails("HOT-1", "ROOM-1");
    }

    @Test
    void hotelDetailsReturnsBadRequestWhenHotelIdMissing() throws Exception {
        mockMvc.perform(get("/details/hotel")
                        .param("roomId", "ROOM-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void hotelDetailsReturnsFullHotelWhenRoomIdOmitted() throws Exception {
        RoomType room1 = new RoomType();
        room1.setRoomId("ROOM-1");
        RoomType room2 = new RoomType();
        room2.setRoomId("ROOM-2");
        when(hotelService.getDetails(anyString(), any())).thenReturn(Hotel.builder()
                .hotelId("HOT-1")
                .roomTypes(List.of(room1, room2))
                .build());

        mockMvc.perform(get("/details/hotel")
                        .param("hotelId", "HOT-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value("HOT-1"))
                .andExpect(jsonPath("$.roomTypes.length()").value(2));

        verify(hotelService).getDetails("HOT-1", null);
    }

    @Test
    void hotelDetailsReturnsNotFoundWhenServiceThrows() throws Exception {
        when(hotelService.getDetails(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mockMvc.perform(get("/details/hotel")
                        .param("hotelId", "HOT-UNKNOWN")
                        .param("roomId", "ROOM-UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void flightDetailsReturnsFlight() throws Exception {
        Flight flight = new Flight();
        flight.setFlightId("FL-1");
        when(flightService.getDetails(anyString())).thenReturn(flight);

        mockMvc.perform(get("/details/flight")
                        .param("flightId", "FL-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value("FL-1"));

        verify(flightService).getDetails("FL-1");
    }

    @Test
    void flightDetailsReturnsBadRequestWhenFlightIdMissing() throws Exception {
        mockMvc.perform(get("/details/flight"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(flightService);
    }

    @Test
    void flightDetailsReturnsNotFoundWhenServiceThrows() throws Exception {
        when(flightService.getDetails(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mockMvc.perform(get("/details/flight")
                        .param("flightId", "FL-UNKNOWN"))
                .andExpect(status().isNotFound());
    }
}
